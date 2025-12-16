package com.budgetwise.budget.catalog.service;

import com.budgetwise.budget.catalog.dto.ProductTableResponse;
import com.budgetwise.budget.catalog.entity.ProductInfo;
import com.budgetwise.budget.catalog.repository.DailyPriceRecordRepository;
import com.budgetwise.budget.catalog.repository.ProductDietaryTagRepository;
import com.budgetwise.budget.catalog.repository.ProductInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Tests for ProductInfoService
 * Validates batch fetching strategy, N+1 query problem prevention, pagination,
 * and data aggregation for product display with tags and market counts.
 * Uses realistic product catalog scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductInfoService Tests")
class ProductInfoServiceTest {

    @Mock
    private ProductInfoRepository productInfoRepository;

    @Mock
    private ProductDietaryTagRepository productDietaryTagRepository;

    @Mock
    private DailyPriceRecordRepository dailyPriceRecordRepository;

    @InjectMocks
    private ProductInfoService productInfoService;

    private Pageable defaultPageable;
    private List<ProductTableResponse> baseProducts;
    private List<ProductDietaryTagRepository.TagProjection> mockTags;
    private List<DailyPriceRecordRepository.MarketCountProjection> mockMarketCounts;

    @BeforeEach
    void setUp() {
        defaultPageable = PageRequest.of(0, 10);

        // Setup base products
        baseProducts = new ArrayList<>();
        baseProducts.add(createProductResponse(1L, "Rice", "Staples", "PH", "Bigas", "kg", ProductInfo.Status.ACTIVE, 45.50, LocalDate.now()));
        baseProducts.add(createProductResponse(2L, "Tomato", "Vegetables", "PH", "Kamatis", "kg", ProductInfo.Status.ACTIVE, 35.00, LocalDate.now()));
        baseProducts.add(createProductResponse(3L, "Chicken", "Meat", "PH", "Manok", "kg", ProductInfo.Status.ACTIVE, 180.00, LocalDate.now()));

        // Setup mock tags
        mockTags = new ArrayList<>();
        mockTags.add(createTagProjection(1L, "Vegan"));
        mockTags.add(createTagProjection(1L, "Organic"));
        mockTags.add(createTagProjection(2L, "Local"));
        mockTags.add(createTagProjection(2L, "Seasonal"));

        // Setup mock market counts
        mockMarketCounts = new ArrayList<>();
        mockMarketCounts.add(createMarketCountProjection(1L, 15L));
        mockMarketCounts.add(createMarketCountProjection(2L, 22L));
        mockMarketCounts.add(createMarketCountProjection(3L, 8L));
    }

    // ==================== HAPPY PATH ====================

    @Test
    @DisplayName("Happy Path: displayProducts with valid pageable - should return enriched products page")
    void displayProducts_ValidPageable_ShouldReturnEnrichedProductsPage() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);

        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        verify(productInfoRepository).displayProductTable(defaultPageable);
        verify(productDietaryTagRepository).findByProductIdIn(anyList());
        verify(dailyPriceRecordRepository).countMarketsByProductIds(anyList());
    }

    @Test
    @DisplayName("Happy Path: displayProducts enriches tags correctly - should aggregate dietary tags by product")
    void displayProducts_EnrichTagsCorrectly_ShouldAggregateDietaryTagsByProduct() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);

        ProductTableResponse rice = result.getContent().get(0);
        ProductTableResponse tomato = result.getContent().get(1);

        assertEquals(2, rice.getDietaryTags().size());
        assertTrue(rice.getDietaryTags().contains("Vegan"));
        assertTrue(rice.getDietaryTags().contains("Organic"));

        assertEquals(2, tomato.getDietaryTags().size());
        assertTrue(tomato.getDietaryTags().contains("Local"));
        assertTrue(tomato.getDietaryTags().contains("Seasonal"));
    }

    @Test
    @DisplayName("Happy Path: displayProducts enriches market counts - should set correct total markets per product")
    void displayProducts_EnrichMarketCountsCorrectly_ShouldSetTotalMarketsPerProduct() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);

        assertEquals(15, result.getContent().get(0).getTotalMarkets());
        assertEquals(22, result.getContent().get(1).getTotalMarkets());
        assertEquals(8, result.getContent().get(2).getTotalMarkets());
    }

    // ==================== BATCH FETCHING STRATEGY ====================

    @Test
    @DisplayName("Batch Fetching: Should fetch all tags in single query - prevents N+1")
    void displayProducts_BatchFetchAllTags_ShouldPreventN1Query() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        productInfoService.displayProducts(defaultPageable);

        ArgumentCaptor<List<Long>> tagCaptor = ArgumentCaptor.forClass(List.class);
        verify(productDietaryTagRepository).findByProductIdIn(tagCaptor.capture());

        List<Long> capturedIds = tagCaptor.getValue();
        assertEquals(3, capturedIds.size());
        assertTrue(capturedIds.contains(1L));
        assertTrue(capturedIds.contains(2L));
        assertTrue(capturedIds.contains(3L));
    }

    @Test
    @DisplayName("Batch Fetching: Should fetch all market counts in single query")
    void displayProducts_BatchFetchMarketCounts_ShouldPreventN1Query() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        productInfoService.displayProducts(defaultPageable);

        ArgumentCaptor<List<Long>> marketCaptor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).countMarketsByProductIds(marketCaptor.capture());

        List<Long> capturedIds = marketCaptor.getValue();
        assertEquals(3, capturedIds.size());
        verify(dailyPriceRecordRepository, times(1)).countMarketsByProductIds(anyList());
    }

    // ==================== EMPTY RESULTS ====================

    @Test
    @DisplayName("Empty Results: displayProducts with empty page - should return empty page without querying related data")
    void displayProducts_EmptyPage_ShouldReturnEmptyPageWithoutQueriesRelatedData() {
        Page<ProductTableResponse> emptyPage = new PageImpl<>(new ArrayList<>(), defaultPageable, 0);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(emptyPage);

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getContent().size());
        verify(productInfoRepository).displayProductTable(defaultPageable);
        verify(productDietaryTagRepository, never()).findByProductIdIn(anyList());
        verify(dailyPriceRecordRepository, never()).countMarketsByProductIds(anyList());
    }

    // ==================== MISSING DATA HANDLING ====================

    @Test
    @DisplayName("Missing Data: Product without tags - should set empty list for dietary tags")
    void displayProducts_ProductWithoutTags_ShouldSetEmptyTagsList() {
        List<ProductTableResponse> productsWithoutTags = new ArrayList<>();
        productsWithoutTags.add(createProductResponse(4L, "Egg", "Proteins", "PH", "Itlog", "tray", ProductInfo.Status.ACTIVE, 65.00, LocalDate.now()));

        Page<ProductTableResponse> productsPage = new PageImpl<>(productsWithoutTags, defaultPageable, 1);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(new ArrayList<>());
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);

        assertEquals(0, result.getContent().get(0).getDietaryTags().size());
        assertTrue(result.getContent().get(0).getDietaryTags().isEmpty());
    }

    @Test
    @DisplayName("Missing Data: Product without market counts - should default to 0")
    void displayProducts_ProductWithoutMarketCounts_ShouldDefaultToZero() {
        List<ProductTableResponse> products = new ArrayList<>();
        products.add(createProductResponse(5L, "Fish", "Seafood", "PH", "Isda", "kg", ProductInfo.Status.ACTIVE, 150.00, LocalDate.now()));

        Page<ProductTableResponse> productsPage = new PageImpl<>(products, defaultPageable, 1);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(new ArrayList<>());
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(new ArrayList<>());

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);

        assertEquals(0, result.getContent().get(0).getTotalMarkets());
    }

    @Test
    @DisplayName("Missing Data: Null market count value - should handle gracefully and default to 0")
    void displayProducts_NullMarketCountValue_ShouldDefaultToZero() {
        List<ProductTableResponse> products = new ArrayList<>();
        products.add(createProductResponse(6L, "Shrimp", "Seafood", "PH", "Hipon", "kg", ProductInfo.Status.ACTIVE, 250.00, LocalDate.now()));

        Page<ProductTableResponse> productsPage = new PageImpl<>(products, defaultPageable, 1);

        List<DailyPriceRecordRepository.MarketCountProjection> nullCountTags = new ArrayList<>();
        nullCountTags.add(createMarketCountProjection(6L, null));

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(new ArrayList<>());
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(nullCountTags);

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);

        assertEquals(0, result.getContent().get(0).getTotalMarkets());
    }

    // ==================== PAGINATION ====================

    @Test
    @DisplayName("Pagination: displayProducts with different page sizes - should respect pageable configuration")
    void displayProducts_DifferentPageSizes_ShouldRespectPageableConfig() {
        Pageable page1 = PageRequest.of(0, 5);
        Pageable page2 = PageRequest.of(1, 5);

        Page<ProductTableResponse> page1Results = new PageImpl<>(baseProducts.subList(0, Math.min(2, baseProducts.size())), page1, 3);
        Page<ProductTableResponse> page2Results = new PageImpl<>(new ArrayList<>(), page2, 3);

        when(productInfoRepository.displayProductTable(page1)).thenReturn(page1Results);
        when(productInfoRepository.displayProductTable(page2)).thenReturn(page2Results);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        Page<ProductTableResponse> result1 = productInfoService.displayProducts(page1);
        Page<ProductTableResponse> result2 = productInfoService.displayProducts(page2);

        assertEquals(0, result1.getNumber());
        assertEquals(1, result2.getNumber());
        verify(productInfoRepository).displayProductTable(page1);
        verify(productInfoRepository).displayProductTable(page2);
    }

    // ==================== DATA INTEGRITY ====================

    @Test
    @DisplayName("Data Integrity: Product details should remain unchanged after enrichment")
    void displayProducts_ProductDetailsPreserved_ShouldKeepOriginalProductData() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 1);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);
        ProductTableResponse enrichedRice = result.getContent().get(0);

        assertEquals(1L, enrichedRice.getId());
        assertEquals("Rice", enrichedRice.getProductName());
        assertEquals("Staples", enrichedRice.getCategory());
        assertEquals("PH", enrichedRice.getOrigin());
        assertEquals("Bigas", enrichedRice.getLocalName());
        assertEquals("kg", enrichedRice.getUnit());
        assertEquals(ProductInfo.Status.ACTIVE, enrichedRice.getStatus());
        assertEquals(45.50, enrichedRice.getPrice());
    }

    @Test
    @DisplayName("Data Integrity: Multiple tags for single product - should aggregate all tags correctly")
    void displayProducts_MultipleTagsPerProduct_ShouldAggregateAllTagsCorrectly() {
        List<ProductDietaryTagRepository.TagProjection> manyTags = new ArrayList<>();
        manyTags.add(createTagProjection(1L, "Vegan"));
        manyTags.add(createTagProjection(1L, "Organic"));
        manyTags.add(createTagProjection(1L, "Local"));
        manyTags.add(createTagProjection(1L, "GlutenFree"));

        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts.subList(0, 1), defaultPageable, 1);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(manyTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);
        ProductTableResponse rice = result.getContent().get(0);

        assertEquals(4, rice.getDietaryTags().size());
        assertTrue(rice.getDietaryTags().contains("Vegan"));
        assertTrue(rice.getDietaryTags().contains("Organic"));
        assertTrue(rice.getDietaryTags().contains("Local"));
        assertTrue(rice.getDietaryTags().contains("GlutenFree"));
    }

    // ==================== TRANSACTIONAL BEHAVIOR ====================

    @Test
    @DisplayName("Transactional: displayProducts should be read-only - no save or update operations")
    void displayProducts_ReadOnlyOperation_ShouldNotPersistData() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        productInfoService.displayProducts(defaultPageable);

        verify(productInfoRepository, never()).save(any());
        verify(productDietaryTagRepository, never()).save(any());
        verify(dailyPriceRecordRepository, never()).save(any());
    }

    // ==================== REPOSITORY INTERACTIONS ====================

    @Test
    @DisplayName("Repository: Verify displayProductTable is called with correct pageable")
    void displayProducts_VerifyRepositoryCallWithCorrectPageable() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        productInfoService.displayProducts(defaultPageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productInfoRepository).displayProductTable(pageableCaptor.capture());

        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber());
        assertEquals(10, captured.getPageSize());
    }

    @Test
    @DisplayName("Repository: Verify correct product IDs are extracted for batch queries")
    void displayProducts_VerifyCorrectProductIdsExtracted() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        productInfoService.displayProducts(defaultPageable);

        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(productDietaryTagRepository).findByProductIdIn(idsCaptor.capture());

        List<Long> capturedIds = idsCaptor.getValue();
        assertEquals(3, capturedIds.size());
        assertTrue(capturedIds.containsAll(Arrays.asList(1L, 2L, 3L)));
    }

    @Test
    @DisplayName("Repository: All three repository methods should be called exactly once")
    void displayProducts_VerifyAllRepositoriesCalledExactlyOnce() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        productInfoService.displayProducts(defaultPageable);

        verify(productInfoRepository, times(1)).displayProductTable(any());
        verify(productDietaryTagRepository, times(1)).findByProductIdIn(anyList());
        verify(dailyPriceRecordRepository, times(1)).countMarketsByProductIds(anyList());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge Case: Single product in result - should handle correctly")
    void displayProducts_SingleProductInResult_ShouldHandleCorrectly() {
        List<ProductTableResponse> singleProduct = new ArrayList<>();
        singleProduct.add(createProductResponse(1L, "Rice", "Staples", "PH", "Bigas", "kg", ProductInfo.Status.ACTIVE, 45.50, LocalDate.now()));

        Page<ProductTableResponse> productsPage = new PageImpl<>(singleProduct, defaultPageable, 1);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);

        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Edge Case: Product with zero price - should preserve price value")
    void displayProducts_ProductWithZeroPrice_ShouldPreserveZeroValue() {
        List<ProductTableResponse> zeroProducts = new ArrayList<>();
        zeroProducts.add(createProductResponse(7L, "Sample", "Test", "PH", "Sample", "kg", ProductInfo.Status.ACTIVE, 0.0, LocalDate.now()));

        Page<ProductTableResponse> productsPage = new PageImpl<>(zeroProducts, defaultPageable, 1);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(new ArrayList<>());
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(new ArrayList<>());

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);

        assertEquals(0.0, result.getContent().get(0).getPrice());
    }

    @Test
    @DisplayName("Edge Case: Very large market count - should handle correctly")
    void displayProducts_VeryLargeMarketCount_ShouldHandleCorrectly() {
        List<ProductTableResponse> products = new ArrayList<>();
        products.add(createProductResponse(8L, "Popular", "General", "PH", "Popular", "kg", ProductInfo.Status.ACTIVE, 100.0, LocalDate.now()));

        Page<ProductTableResponse> productsPage = new PageImpl<>(products, defaultPageable, 1);

        List<DailyPriceRecordRepository.MarketCountProjection> largeCount = new ArrayList<>();
        largeCount.add(createMarketCountProjection(8L, 9999L));

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(new ArrayList<>());
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(largeCount);

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);

        assertEquals(9999, result.getContent().get(0).getTotalMarkets());
    }

    // ==================== EXCEPTION CASES ====================

    @Test
    @DisplayName("Exception: displayProductTable throws exception - should propagate exception")
    void displayProducts_RepositoryThrowsException_ShouldPropagateException() {
        when(productInfoRepository.displayProductTable(defaultPageable))
                .thenThrow(new RuntimeException("Database connection error"));

        assertThrows(RuntimeException.class, () -> productInfoService.displayProducts(defaultPageable));
    }

    @Test
    @DisplayName("Exception: Tag repository throws exception - should propagate exception")
    void displayProducts_TagRepositoryThrowsException_ShouldPropagateException() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList()))
                .thenThrow(new RuntimeException("Tag fetch error"));

        assertThrows(RuntimeException.class, () -> productInfoService.displayProducts(defaultPageable));
    }

    @Test
    @DisplayName("Exception: Market count repository throws exception - should propagate exception")
    void displayProducts_MarketCountRepositoryThrowsException_ShouldPropagateException() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList()))
                .thenThrow(new RuntimeException("Market count fetch error"));

        assertThrows(RuntimeException.class, () -> productInfoService.displayProducts(defaultPageable));
    }

    // ==================== REAL WORLD SCENARIOS ====================

    @Test
    @DisplayName("Real World: High pagination load - should handle multiple page requests sequentially")
    void displayProducts_HighPaginationLoad_ShouldHandleMultiplePageRequests() {
        Pageable page0 = PageRequest.of(0, 10);
        Pageable page1 = PageRequest.of(1, 10);
        Pageable page2 = PageRequest.of(2, 10);

        Page<ProductTableResponse> page0Results = new PageImpl<>(baseProducts, page0, 25);
        Page<ProductTableResponse> page1Results = new PageImpl<>(baseProducts, page1, 25);
        Page<ProductTableResponse> page2Results = new PageImpl<>(baseProducts.subList(0, 1), page2, 25);

        when(productInfoRepository.displayProductTable(any(Pageable.class)))
                .thenReturn(page0Results)
                .thenReturn(page1Results)
                .thenReturn(page2Results);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        Page<ProductTableResponse> result0 = productInfoService.displayProducts(page0);
        Page<ProductTableResponse> result1 = productInfoService.displayProducts(page1);
        Page<ProductTableResponse> result2 = productInfoService.displayProducts(page2);

        assertEquals(0, result0.getNumber());
        assertEquals(1, result1.getNumber());
        assertEquals(2, result2.getNumber());
        verify(productInfoRepository, times(3)).displayProductTable(any(Pageable.class));
    }

    @Test
    @DisplayName("Real World: Mixed product statuses - should display correctly regardless of status")
    void displayProducts_MixedProductStatuses_ShouldDisplayCorrectly() {
        List<ProductTableResponse> mixedProducts = new ArrayList<>();
        mixedProducts.add(createProductResponse(1L, "Rice", "Staples", "PH", "Bigas", "kg", ProductInfo.Status.ACTIVE, 45.50, LocalDate.now()));
        mixedProducts.add(createProductResponse(2L, "OldProduct", "Test", "PH", "Luma", "kg", ProductInfo.Status.INACTIVE, 10.00, LocalDate.now().minusMonths(1)));
        mixedProducts.add(createProductResponse(3L, "Chicken", "Meat", "PH", "Manok", "kg", ProductInfo.Status.ACTIVE, 180.00, LocalDate.now()));

        Page<ProductTableResponse> productsPage = new PageImpl<>(mixedProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        Page<ProductTableResponse> result = productInfoService.displayProducts(defaultPageable);

        assertEquals(ProductInfo.Status.ACTIVE, result.getContent().get(0).getStatus());
        assertEquals(ProductInfo.Status.INACTIVE, result.getContent().get(1).getStatus());
        assertEquals(ProductInfo.Status.ACTIVE, result.getContent().get(2).getStatus());
    }

    @Test
    @DisplayName("Real World: Concurrent display requests - should maintain data consistency")
    void displayProducts_ConcurrentRequests_ShouldMaintainDataConsistency() {
        Page<ProductTableResponse> productsPage = new PageImpl<>(baseProducts, defaultPageable, 3);

        when(productInfoRepository.displayProductTable(defaultPageable)).thenReturn(productsPage);
        when(productDietaryTagRepository.findByProductIdIn(anyList())).thenReturn(mockTags);
        when(dailyPriceRecordRepository.countMarketsByProductIds(anyList())).thenReturn(mockMarketCounts);

        Page<ProductTableResponse> result1 = productInfoService.displayProducts(defaultPageable);
        Page<ProductTableResponse> result2 = productInfoService.displayProducts(defaultPageable);
        Page<ProductTableResponse> result3 = productInfoService.displayProducts(defaultPageable);

        assertEquals(result1.getContent().get(0).getDietaryTags(), result2.getContent().get(0).getDietaryTags());
        assertEquals(result2.getContent().get(0).getDietaryTags(), result3.getContent().get(0).getDietaryTags());
        verify(productInfoRepository, times(3)).displayProductTable(defaultPageable);
    }

    // ==================== HELPER METHODS ====================

    private ProductTableResponse createProductResponse(Long id, String productName, String category, String origin,
                                                       String localName, String unit, ProductInfo.Status status,
                                                       Double price, LocalDate lastUpdated) {
        ProductTableResponse response = new ProductTableResponse();
        response.setId(id);
        response.setProductName(productName);
        response.setCategory(category);
        response.setOrigin(origin);
        response.setLocalName(localName);
        response.setUnit(unit);
        response.setStatus(status);
        response.setPrice(price);
        response.setLastUpdated(lastUpdated);
        return response;
    }

    private ProductDietaryTagRepository.TagProjection createTagProjection(Long productId, String tag) {
        return new ProductDietaryTagRepository.TagProjection() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public String getDietaryTag() {
                return tag;
            }
        };
    }

    private DailyPriceRecordRepository.MarketCountProjection createMarketCountProjection(Long productId, Long marketCount) {
        return new DailyPriceRecordRepository.MarketCountProjection() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public Long getTotalMarkets() {
                return marketCount;
            }
        };
    }
}
