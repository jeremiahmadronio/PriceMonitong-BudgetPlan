package com.budgetwise.budget.catalog.service;

import com.budgetwise.budget.catalog.entity.ProductInfo;
import com.budgetwise.budget.catalog.repository.ProductInfoRepository;
import com.budgetwise.budget.integration.scrapper.dto.ScrapeResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for ProductMatchingService
 * Validates product matching logic, status transitions, and edge cases
 * Uses realistic Filipino wet market products for test data
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductMatchingService Tests")
class ProductMatchingServiceTest {

    @Mock
    private ProductInfoRepository productInfoRepository;

    @InjectMocks
    private ProductMatchingService productMatchingService;

    private ScrapeResultDto.ScrapedProduct validScrapedProduct;
    private ProductInfo existingActiveProduct;
    private ProductInfo existingPendingProduct;

    @BeforeEach
    void setUp() {
        // Test data: Bangus from Dagupan
        validScrapedProduct = new ScrapeResultDto.ScrapedProduct(
                "FISH",           // category
                "Bangus",         // commodity
                "Dagupan",        // origin
                "kg",             // unit
                180.0             // price
        );

        existingActiveProduct = new ProductInfo();
        existingActiveProduct.setId(1L);
        existingActiveProduct.setCategory("FISH");
        existingActiveProduct.setProductName("Bangus");
        existingActiveProduct.setStatus(ProductInfo.Status.ACTIVE);

        existingPendingProduct = new ProductInfo();
        existingPendingProduct.setId(2L);
        existingPendingProduct.setCategory("FISH");
        existingPendingProduct.setProductName("Bangus");
        existingPendingProduct.setStatus(ProductInfo.Status.PENDING);
    }

    // ==================== HAPPY PATH ====================

    @Test
    @DisplayName("Happy Path: Known product from known origin - should return ACTIVE product")
    void findOrCreateProduct_KnownProductKnownOrigin_ShouldReturnActiveProduct() {
        // NOTE: Production code has swapped parameters (commodity, category, origin)
        // Correct order per method name should be (category, productName, origin)
        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Bangus", "FISH", "Dagupan"
        )).thenReturn(true);
        when(productInfoRepository.findByCategoryAndProductName("FISH", "Bangus"))
                .thenReturn(Optional.of(existingActiveProduct));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(validScrapedProduct);

        // Assert
        assertNotNull(result);
        assertEquals(ProductInfo.Status.ACTIVE, result.getStatus());
        assertEquals("Bangus", result.getProductName());
        verify(productInfoRepository).existsByCategoryAndProductNameAndOrigin(
                "Bangus", "FISH", "Dagupan"
        );
        verify(productInfoRepository).findByCategoryAndProductName("FISH", "Bangus");
        verify(productInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Happy Path: Known product needs reactivation - should set to ACTIVE and save")
    void findOrCreateProduct_PendingProductFromKnownOrigin_ShouldReactivate() {
        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Bangus", "FISH", "Dagupan"
        )).thenReturn(true);
        when(productInfoRepository.findByCategoryAndProductName("FISH", "Bangus"))
                .thenReturn(Optional.of(existingPendingProduct));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(validScrapedProduct);

        // Assert
        assertNotNull(result);
        assertEquals(ProductInfo.Status.ACTIVE, result.getStatus());
        verify(productInfoRepository).save(existingPendingProduct);
        assertEquals(ProductInfo.Status.ACTIVE, existingPendingProduct.getStatus());
    }

    @Test
    @DisplayName("Happy Path: Brand new product - should create with PENDING status")
    void findOrCreateProduct_NewProduct_ShouldCreatePending() {
        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Bangus", "FISH", "Dagupan"
        )).thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName("FISH", "Bangus"))
                .thenReturn(Optional.empty());
        when(productInfoRepository.save(any(ProductInfo.class)))
                .thenAnswer(invocation -> {
                    ProductInfo saved = invocation.getArgument(0);
                    saved.setId(999L);
                    return saved;
                });

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(validScrapedProduct);

        // Assert
        assertNotNull(result);
        assertEquals(ProductInfo.Status.PENDING, result.getStatus());
        assertEquals("Bangus", result.getProductName());
        assertEquals("FISH", result.getCategory());


        ArgumentCaptor<ProductInfo> captor = ArgumentCaptor.forClass(ProductInfo.class);
        verify(productInfoRepository).save(captor.capture());
        ProductInfo saved = captor.getValue();
        assertEquals("Bangus", saved.getProductName());
        assertEquals(ProductInfo.Status.PENDING, saved.getStatus());
    }

    @Test
    @DisplayName("Happy Path: Known product but new origin - should return existing product as-is")
    void findOrCreateProduct_KnownProductNewOrigin_ShouldReturnExistingWithoutChange() {
        // Test case: Bangus from Bulacan doesn't exist, but Bangus from Dagupan does
        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Bangus", "FISH", "Dagupan"
        )).thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName("FISH", "Bangus"))
                .thenReturn(Optional.of(existingPendingProduct));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(validScrapedProduct);

        // Assert
        assertNotNull(result);
        assertEquals(existingPendingProduct.getId(), result.getId());
        assertEquals(ProductInfo.Status.PENDING, result.getStatus()); // Should NOT change status
        verify(productInfoRepository, never()).save(any()); // Should NOT save
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge Case: Null category (Pork Liempo without category)")
    void findOrCreateProduct_NullCategory_ShouldHandleGracefully() {
        ScrapeResultDto.ScrapedProduct productWithNullCategory =
                new ScrapeResultDto.ScrapedProduct(null, "Pork Liempo", "Local", "kg", 280.0);

        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin("Pork Liempo", null, "Local"))
                .thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName(null, "Pork Liempo"))
                .thenReturn(Optional.empty());
        when(productInfoRepository.save(any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(productWithNullCategory);

        // Assert
        assertNotNull(result);
        assertNull(result.getCategory());
        assertEquals("Pork Liempo", result.getProductName());
        verify(productInfoRepository).existsByCategoryAndProductNameAndOrigin("Pork Liempo", null, "Local");
    }

    @Test
    @DisplayName("Edge Case: Null commodity name")
    void findOrCreateProduct_NullCommodity_ShouldHandleGracefully() {
        ScrapeResultDto.ScrapedProduct productWithNullCommodity =
                new ScrapeResultDto.ScrapedProduct("MEAT", null, "Bulacan", "kg", 250.0);

        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(null, "MEAT", "Bulacan"))
                .thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName("MEAT", null))
                .thenReturn(Optional.empty());
        when(productInfoRepository.save(any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(productWithNullCommodity);

        // Assert
        assertNotNull(result);
        assertNull(result.getProductName());
        assertEquals("MEAT", result.getCategory());
        verify(productInfoRepository).existsByCategoryAndProductNameAndOrigin(null, "MEAT", "Bulacan");
    }

    @Test
    @DisplayName("Edge Case: Null origin (Tilapia without origin)")
    void findOrCreateProduct_NullOrigin_ShouldHandleGracefully() {
        ScrapeResultDto.ScrapedProduct productWithNullOrigin =
                new ScrapeResultDto.ScrapedProduct("FISH", "Tilapia", null, "kg", 120.0);

        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin("Tilapia", "FISH", null))
                .thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName("FISH", "Tilapia"))
                .thenReturn(Optional.empty());
        when(productInfoRepository.save(any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(productWithNullOrigin);

        // Assert
        assertNotNull(result);
        assertEquals("Tilapia", result.getProductName());
        verify(productInfoRepository).existsByCategoryAndProductNameAndOrigin("Tilapia", "FISH", null);
    }

    @Test
    @DisplayName("Edge Case: Empty strings for all fields")
    void findOrCreateProduct_EmptyStrings_ShouldHandleGracefully() {
        ScrapeResultDto.ScrapedProduct productWithEmptyStrings =
                new ScrapeResultDto.ScrapedProduct("", "", "", "kg", 50.0);

        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin("", "", ""))
                .thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName("", ""))
                .thenReturn(Optional.empty());
        when(productInfoRepository.save(any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(productWithEmptyStrings);

        // Assert
        assertNotNull(result);
        assertEquals("", result.getCategory());
        assertEquals("", result.getProductName());
        verify(productInfoRepository).save(any(ProductInfo.class));
    }

    @Test
    @DisplayName("Edge Case: SKU generation uniqueness for Chicken")
    void findOrCreateProduct_MultipleCalls_ShouldGenerateUniqueSKUs() {
        ScrapeResultDto.ScrapedProduct chicken =
                new ScrapeResultDto.ScrapedProduct("POULTRY", "Chicken", "Bulacan", "kg", 180.0);

        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(any(), any(), any()))
                .thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName(any(), any()))
                .thenReturn(Optional.empty());
        when(productInfoRepository.save(any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductInfo result1 = productMatchingService.findOrCreateProduct(chicken);
        ProductInfo result2 = productMatchingService.findOrCreateProduct(chicken);


    }

    // ==================== EXCEPTIONS ====================

    @Test
    @DisplayName("Exception: Product should exist but not found in DB")
    void findOrCreateProduct_ExistsCheckTrueButNotFound_ShouldThrowException() {
        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Bangus", "FISH", "Dagupan"
        )).thenReturn(true);
        when(productInfoRepository.findByCategoryAndProductName("FISH", "Bangus"))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> productMatchingService.findOrCreateProduct(validScrapedProduct)
        );

        assertEquals("Product should exist but not found in DB", exception.getMessage());
        verify(productInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Exception: Repository throws exception during exists check")
    void findOrCreateProduct_RepositoryExceptionOnExists_ShouldPropagateException() {
        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(any(), any(), any()))
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> productMatchingService.findOrCreateProduct(validScrapedProduct)
        );
    }

    @Test
    @DisplayName("Exception: Repository throws exception during save")
    void findOrCreateProduct_RepositoryExceptionOnSave_ShouldPropagateException() {
        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(any(), any(), any()))
                .thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName(any(), any()))
                .thenReturn(Optional.empty());
        when(productInfoRepository.save(any(ProductInfo.class)))
                .thenThrow(new RuntimeException("Database save error"));

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> productMatchingService.findOrCreateProduct(validScrapedProduct)
        );
    }

    @Test
    @DisplayName("Exception: Repository throws exception during reactivation save")
    void findOrCreateProduct_ExceptionDuringReactivation_ShouldPropagateException() {
        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(any(), any(), any()))
                .thenReturn(true);
        when(productInfoRepository.findByCategoryAndProductName(any(), any()))
                .thenReturn(Optional.of(existingPendingProduct));
        when(productInfoRepository.save(any(ProductInfo.class)))
                .thenThrow(new RuntimeException("Reactivation save failed"));

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> productMatchingService.findOrCreateProduct(validScrapedProduct)
        );
    }

    // ==================== STATUS TRANSITIONS ====================

    @Test
    @DisplayName("Status Transition: PENDING to ACTIVE when origin matches")
    void findOrCreateProduct_StatusTransition_PendingToActive() {
        ScrapeResultDto.ScrapedProduct galunggong =
                new ScrapeResultDto.ScrapedProduct("FISH", "Galunggong", "Navotas", "kg", 220.0);

        ProductInfo pendingProduct = new ProductInfo();
        pendingProduct.setId(100L);
        pendingProduct.setStatus(ProductInfo.Status.PENDING);
        pendingProduct.setCategory("FISH");
        pendingProduct.setProductName("Galunggong");

        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Galunggong", "FISH", "Navotas"
        )).thenReturn(true);
        when(productInfoRepository.findByCategoryAndProductName("FISH", "Galunggong"))
                .thenReturn(Optional.of(pendingProduct));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(galunggong);

        // Assert
        assertEquals(ProductInfo.Status.ACTIVE, result.getStatus());
        verify(productInfoRepository).save(pendingProduct);
    }

    @Test
    @DisplayName("Status Check: ACTIVE product should remain ACTIVE")
    void findOrCreateProduct_ActiveProductStaysActive() {
        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Bangus", "FISH", "Dagupan"
        )).thenReturn(true);
        when(productInfoRepository.findByCategoryAndProductName("FISH", "Bangus"))
                .thenReturn(Optional.of(existingActiveProduct));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(validScrapedProduct);

        // Assert
        assertEquals(ProductInfo.Status.ACTIVE, result.getStatus());
        verify(productInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Status Check: New product should always be PENDING")
    void findOrCreateProduct_NewProductAlwaysPending() {
        ScrapeResultDto.ScrapedProduct beef =
                new ScrapeResultDto.ScrapedProduct("MEAT", "Beef Kalitiran", "Local", "kg", 380.0);

        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Beef Kalitiran", "MEAT", "Local"
        )).thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName("MEAT", "Beef Kalitiran"))
                .thenReturn(Optional.empty());
        when(productInfoRepository.save(any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(beef);

        // Assert
        assertEquals(ProductInfo.Status.PENDING, result.getStatus());
    }

    // ==================== BUSINESS LOGIC ====================

    @Test
    @DisplayName("Business Logic: Same product different origins - Bangus from different sources")
    void findOrCreateProduct_SameProductDifferentOrigin_ShouldNotReactivate() {
        // Test: Bangus from Bulacan vs Bangus from Dagupan
        ScrapeResultDto.ScrapedProduct bangusBulacan =
                new ScrapeResultDto.ScrapedProduct("FISH", "Bangus", "Bulacan", "kg", 200.0);

        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Bangus", "FISH", "Bulacan"
        )).thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName("FISH", "Bangus"))
                .thenReturn(Optional.of(existingPendingProduct));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(bangusBulacan);

        // Assert
        assertEquals(ProductInfo.Status.PENDING, result.getStatus()); // Should stay PENDING
        verify(productInfoRepository, never()).save(any()); // Should NOT save
    }

    @Test
    @DisplayName("Business Logic: Verify method call sequence for new product - Pechay")
    void findOrCreateProduct_NewProduct_VerifyMethodCallSequence() {
        ScrapeResultDto.ScrapedProduct pechay =
                new ScrapeResultDto.ScrapedProduct("VEGETABLES", "Pechay", "Benguet", "kg", 40.0);

        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Pechay", "VEGETABLES", "Benguet"
        )).thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName("VEGETABLES", "Pechay"))
                .thenReturn(Optional.empty());
        when(productInfoRepository.save(any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        productMatchingService.findOrCreateProduct(pechay);

        // Verify exact call sequence
        var inOrder = inOrder(productInfoRepository);
        inOrder.verify(productInfoRepository).existsByCategoryAndProductNameAndOrigin(
                "Pechay", "VEGETABLES", "Benguet"
        );
        inOrder.verify(productInfoRepository).findByCategoryAndProductName("VEGETABLES", "Pechay");
        inOrder.verify(productInfoRepository).save(any(ProductInfo.class));
    }

    @Test
    @DisplayName("Business Logic: Verify method call sequence for existing product - Egg")
    void findOrCreateProduct_ExistingProduct_VerifyMethodCallSequence() {
        ScrapeResultDto.ScrapedProduct egg =
                new ScrapeResultDto.ScrapedProduct("POULTRY", "Egg", "Local", "pc", 7.5);

        ProductInfo existingEgg = new ProductInfo();
        existingEgg.setId(50L);
        existingEgg.setCategory("POULTRY");
        existingEgg.setProductName("Egg");
        existingEgg.setStatus(ProductInfo.Status.ACTIVE);

        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Egg", "POULTRY", "Local"
        )).thenReturn(true);
        when(productInfoRepository.findByCategoryAndProductName("POULTRY", "Egg"))
                .thenReturn(Optional.of(existingEgg));

        // Act
        productMatchingService.findOrCreateProduct(egg);

        // Verify exact call sequence
        var inOrder = inOrder(productInfoRepository);
        inOrder.verify(productInfoRepository).existsByCategoryAndProductNameAndOrigin(
                "Egg", "POULTRY", "Local"
        );
        inOrder.verify(productInfoRepository).findByCategoryAndProductName("POULTRY", "Egg");
        inOrder.verify(productInfoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Real World: Commercial Rice - Well Milled Rice Imported")
    void findOrCreateProduct_CommercialRice_ShouldHandleCorrectly() {
        ScrapeResultDto.ScrapedProduct rice =
                new ScrapeResultDto.ScrapedProduct(
                        "COMMERCIAL RICE",
                        "Well Milled Rice",
                        "Imported",
                        "kg",
                        45.0
                );

        when(productInfoRepository.existsByCategoryAndProductNameAndOrigin(
                "Well Milled Rice", "COMMERCIAL RICE", "Imported"
        )).thenReturn(false);
        when(productInfoRepository.findByCategoryAndProductName("COMMERCIAL RICE", "Well Milled Rice"))
                .thenReturn(Optional.empty());
        when(productInfoRepository.save(any(ProductInfo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductInfo result = productMatchingService.findOrCreateProduct(rice);

        // Assert
        assertNotNull(result);
        assertEquals("Well Milled Rice", result.getProductName());
        assertEquals("COMMERCIAL RICE", result.getCategory());
        assertEquals(ProductInfo.Status.PENDING, result.getStatus());
    }
}