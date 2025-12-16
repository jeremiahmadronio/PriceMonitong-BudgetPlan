package com.budgetwise.budget.catalog.service;

import com.budgetwise.budget.market.entity.MarketLocation;
import com.budgetwise.budget.catalog.entity.PriceReport;
import com.budgetwise.budget.catalog.entity.ProductInfo;
import com.budgetwise.budget.integration.scrapper.dto.ScrapeResultDto;
import com.budgetwise.budget.market.service.MarketLocationResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for ProductInfoService
 * Validates orchestration logic, transaction flow, and integration between services
 * Uses realistic Filipino wet market products and markets for test data
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductInfoService Tests")
class CatalogIngestionServiceTest {

    @Mock
    private PriceReportProcessingService priceReportService;

    @Mock
    private DailyPriceIngestionService dailyPriceRecordService;

    @Mock
    private MarketLocationResolver marketLocationService;

    @Mock
    private ProductMatchingService productMatchingService;

    @InjectMocks
    private CatalogIngestionService productInfoService;

    private ScrapeResultDto validScrapeResult;
    private PriceReport mockPriceReport;
    private List<MarketLocation> mockMarkets;
    private ProductInfo mockProductInfo;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.of(2025, 12, 15, 8, 0, 0);

        // Mock Price Report
        mockPriceReport = new PriceReport();
        mockPriceReport.setId(1L);
        mockPriceReport.setDateProcessed(testDateTime);

        // Mock Markets - Commonwealth, Balintawak, Muñoz
        MarketLocation commonwealth = new MarketLocation();
        commonwealth.setId(1L);
        commonwealth.setMarketLocation("Commonwealth Market");

        MarketLocation balintawak = new MarketLocation();
        balintawak.setId(2L);
        balintawak.setMarketLocation("Balintawak Market");

        MarketLocation munoz = new MarketLocation();
        munoz.setId(3L);
        munoz.setMarketLocation("Farmers Market Cubao (Muñoz)");

        mockMarkets = Arrays.asList(commonwealth, balintawak, munoz);

        // Mock Product Info
        mockProductInfo = new ProductInfo();
        mockProductInfo.setId(10L);
        mockProductInfo.setCategory("FISH");
        mockProductInfo.setProductName("Bangus");
        mockProductInfo.setStatus(ProductInfo.Status.ACTIVE);

        // Valid Scrape Result with multiple products
        List<ScrapeResultDto.ScrapedProduct> products = Arrays.asList(
                new ScrapeResultDto.ScrapedProduct("FISH", "Bangus", "Dagupan", "kg", 180.0),
                new ScrapeResultDto.ScrapedProduct("FISH", "Galunggong", "Navotas", "kg", 220.0),
                new ScrapeResultDto.ScrapedProduct("VEGETABLES", "Kamatis", "Batangas", "kg", 60.0)
        );

        List<String> markets = Arrays.asList("Commonwealth Market", "Balintawak Market", "Farmers Market Cubao (Muñoz)");

        validScrapeResult = new ScrapeResultDto("success", "2025-12-15", "https://example.com", markets, products);
    }

    // ==================== HAPPY PATH ====================

    @Test
    @DisplayName("Happy Path: Process complete scrape result successfully")
    void processAndSaveScrapeResult_ValidData_ShouldProcessSuccessfully() {
        // Arrange
        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(validScrapeResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(validScrapeResult.coveredMarkets())).thenReturn(mockMarkets);
        when(productMatchingService.findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class)))
                .thenReturn(mockProductInfo);

        // Act
        productInfoService.processAndSaveScrapeResult(validScrapeResult);

        // Assert
        verify(priceReportService).reportExists("2025-12-15");
        verify(priceReportService).createFromScrapeResult(validScrapeResult);
        verify(marketLocationService).findOrCreateMarket(validScrapeResult.coveredMarkets());
        verify(productMatchingService, times(3)).findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class));
        verify(dailyPriceRecordService, times(3)).createRecordForAllMarkets(
                any(ScrapeResultDto.ScrapedProduct.class),
                eq(mockProductInfo),
                eq(mockPriceReport),
                eq(mockMarkets)
        );
    }

    @Test
    @DisplayName("Happy Path: Single product with three markets - verify record creation count")
    void processAndSaveScrapeResult_SingleProduct_ShouldCreateRecordsForAllMarkets() {
        // Arrange - Single product scenario
        List<ScrapeResultDto.ScrapedProduct> singleProduct = Collections.singletonList(
                new ScrapeResultDto.ScrapedProduct("FISH", "Bangus", "Dagupan", "kg", 180.0)
        );
        ScrapeResultDto singleProductResult = new ScrapeResultDto(
                "success",
                "2025-12-15",
                "https://example.com",
                validScrapeResult.coveredMarkets(),
                singleProduct
        );

        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(singleProductResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(singleProductResult.coveredMarkets())).thenReturn(mockMarkets);
        when(productMatchingService.findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class)))
                .thenReturn(mockProductInfo);

        // Act
        productInfoService.processAndSaveScrapeResult(singleProductResult);

        // Assert
        verify(dailyPriceRecordService, times(1)).createRecordForAllMarkets(
                any(ScrapeResultDto.ScrapedProduct.class),
                eq(mockProductInfo),
                eq(mockPriceReport),
                eq(mockMarkets)
        );
    }

    @Test
    @DisplayName("Happy Path: Verify correct order of service calls")
    void processAndSaveScrapeResult_ValidData_ShouldCallServicesInCorrectOrder() {
        // Arrange
        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(validScrapeResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(validScrapeResult.coveredMarkets())).thenReturn(mockMarkets);
        when(productMatchingService.findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class)))
                .thenReturn(mockProductInfo);

        // Act
        productInfoService.processAndSaveScrapeResult(validScrapeResult);

        // Assert - Verify call order
        var inOrder = inOrder(priceReportService, marketLocationService, productMatchingService, dailyPriceRecordService);
        inOrder.verify(priceReportService).reportExists("2025-12-15");
        inOrder.verify(priceReportService).createFromScrapeResult(validScrapeResult);
        inOrder.verify(marketLocationService).findOrCreateMarket(validScrapeResult.coveredMarkets());
        inOrder.verify(productMatchingService).findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class));
        inOrder.verify(dailyPriceRecordService).createRecordForAllMarkets(
                any(ScrapeResultDto.ScrapedProduct.class),
                any(ProductInfo.class),
                any(PriceReport.class),
                any(List.class)
        );
    }

    // ==================== DUPLICATE PREVENTION ====================

    @Test
    @DisplayName("Duplicate Prevention: Existing report - should skip processing")
    void processAndSaveScrapeResult_ReportExists_ShouldSkipProcessing() {
        // Arrange
        when(priceReportService.reportExists("2025-12-15")).thenReturn(true);

        // Act
        productInfoService.processAndSaveScrapeResult(validScrapeResult);

        // Assert
        verify(priceReportService).reportExists("2025-12-15");
        verify(priceReportService, never()).createFromScrapeResult(any());
        verify(marketLocationService, never()).findOrCreateMarket(any());
        verify(productMatchingService, never()).findOrCreateProduct(any());
        verify(dailyPriceRecordService, never()).createRecordForAllMarkets(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Duplicate Prevention: Report exists for same date - early exit")
    void processAndSaveScrapeResult_DuplicateDate_ShouldExitEarly() {
        // Arrange
        ScrapeResultDto duplicateResult = new ScrapeResultDto(
                "success",
                "2025-12-10",
                "https://example.com",
                validScrapeResult.coveredMarkets(),
                validScrapeResult.products()
        );

        when(priceReportService.reportExists("2025-12-10")).thenReturn(true);

        // Act
        productInfoService.processAndSaveScrapeResult(duplicateResult);

        // Assert - Only reportExists should be called
        verify(priceReportService, times(1)).reportExists("2025-12-10");
        verifyNoMoreInteractions(priceReportService);
        verifyNoInteractions(marketLocationService);
        verifyNoInteractions(productMatchingService);
        verifyNoInteractions(dailyPriceRecordService);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge Case: Null products list - should skip processing")
    void processAndSaveScrapeResult_NullProducts_ShouldSkipProcessing() {
        // Arrange
        ScrapeResultDto nullProductsResult = new ScrapeResultDto(
                "success",
                "2025-12-15",
                "https://example.com",
                validScrapeResult.coveredMarkets(),
                null
        );

        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(nullProductsResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(nullProductsResult.coveredMarkets())).thenReturn(mockMarkets);

        // Act
        productInfoService.processAndSaveScrapeResult(nullProductsResult);

        // Assert
        verify(priceReportService).reportExists("2025-12-15");
        verify(priceReportService).createFromScrapeResult(nullProductsResult);
        verify(marketLocationService).findOrCreateMarket(nullProductsResult.coveredMarkets());
        verify(productMatchingService, never()).findOrCreateProduct(any());
        verify(dailyPriceRecordService, never()).createRecordForAllMarkets(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Edge Case: Empty products list - should skip processing")
    void processAndSaveScrapeResult_EmptyProducts_ShouldSkipProcessing() {
        // Arrange
        ScrapeResultDto emptyProductsResult = new ScrapeResultDto(
                "success",
                "2025-12-15",
                "https://example.com",
                validScrapeResult.coveredMarkets(),
                Collections.emptyList()
        );

        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(emptyProductsResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(emptyProductsResult.coveredMarkets())).thenReturn(mockMarkets);

        // Act
        productInfoService.processAndSaveScrapeResult(emptyProductsResult);

        // Assert
        verify(priceReportService).reportExists("2025-12-15");
        verify(priceReportService).createFromScrapeResult(emptyProductsResult);
        verify(marketLocationService).findOrCreateMarket(emptyProductsResult.coveredMarkets());
        verify(productMatchingService, never()).findOrCreateProduct(any());
        verify(dailyPriceRecordService, never()).createRecordForAllMarkets(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Edge Case: Empty markets list - should still process products")
    void processAndSaveScrapeResult_EmptyMarkets_ShouldStillProcessProducts() {
        // Arrange
        List<MarketLocation> emptyMarkets = Collections.emptyList();
        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(validScrapeResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(validScrapeResult.coveredMarkets())).thenReturn(emptyMarkets);
        when(productMatchingService.findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class)))
                .thenReturn(mockProductInfo);

        // Act
        productInfoService.processAndSaveScrapeResult(validScrapeResult);

        // Assert
        verify(productMatchingService, times(3)).findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class));
        verify(dailyPriceRecordService, times(3)).createRecordForAllMarkets(
                any(ScrapeResultDto.ScrapedProduct.class),
                eq(mockProductInfo),
                eq(mockPriceReport),
                eq(emptyMarkets)
        );
    }

    @Test
    @DisplayName("Edge Case: Single market, multiple products")
    void processAndSaveScrapeResult_SingleMarket_ShouldProcessAllProducts() {
        // Arrange
        List<MarketLocation> singleMarket = Collections.singletonList(mockMarkets.get(0));
        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(validScrapeResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(validScrapeResult.coveredMarkets())).thenReturn(singleMarket);
        when(productMatchingService.findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class)))
                .thenReturn(mockProductInfo);

        // Act
        productInfoService.processAndSaveScrapeResult(validScrapeResult);

        // Assert
        verify(productMatchingService, times(3)).findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class));
        verify(dailyPriceRecordService, times(3)).createRecordForAllMarkets(
                any(ScrapeResultDto.ScrapedProduct.class),
                eq(mockProductInfo),
                eq(mockPriceReport),
                eq(singleMarket)
        );
    }

    // ==================== EXCEPTION HANDLING ====================

    @Test
    @DisplayName("Exception: PriceReportService throws exception during report creation")
    void processAndSaveScrapeResult_ReportCreationFails_ShouldPropagateException() {
        // Arrange
        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(validScrapeResult))
                .thenThrow(new RuntimeException("Failed to create price report"));

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> productInfoService.processAndSaveScrapeResult(validScrapeResult)
        );

        verify(marketLocationService, never()).findOrCreateMarket(any());
        verify(productMatchingService, never()).findOrCreateProduct(any());
    }

    @Test
    @DisplayName("Exception: MarketLocationService throws exception")
    void processAndSaveScrapeResult_MarketResolutionFails_ShouldPropagateException() {
        // Arrange
        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(validScrapeResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(validScrapeResult.coveredMarkets()))
                .thenThrow(new RuntimeException("Failed to resolve markets"));

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> productInfoService.processAndSaveScrapeResult(validScrapeResult)
        );

        verify(productMatchingService, never()).findOrCreateProduct(any());
        verify(dailyPriceRecordService, never()).createRecordForAllMarkets(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Exception: ProductMatchingService throws exception during product matching")
    void processAndSaveScrapeResult_ProductMatchingFails_ShouldPropagateException() {
        // Arrange
        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(validScrapeResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(validScrapeResult.coveredMarkets())).thenReturn(mockMarkets);
        when(productMatchingService.findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class)))
                .thenThrow(new RuntimeException("Product matching failed"));

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> productInfoService.processAndSaveScrapeResult(validScrapeResult)
        );
    }

    @Test
    @DisplayName("Exception: DailyPriceRecordService throws exception during record creation")
    void processAndSaveScrapeResult_RecordCreationFails_ShouldPropagateException() {
        // Arrange
        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(validScrapeResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(validScrapeResult.coveredMarkets())).thenReturn(mockMarkets);
        when(productMatchingService.findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class)))
                .thenReturn(mockProductInfo);
        doThrow(new RuntimeException("Failed to create price records"))
                .when(dailyPriceRecordService).createRecordForAllMarkets(
                        any(ScrapeResultDto.ScrapedProduct.class),
                        any(ProductInfo.class),
                        any(PriceReport.class),
                        any(List.class)
                );

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> productInfoService.processAndSaveScrapeResult(validScrapeResult)
        );
    }

    // ==================== BUSINESS LOGIC ====================

    @Test
    @DisplayName("Business Logic: Verify product processing with correct parameters")
    void processAndSaveScrapeResult_ValidProducts_ShouldPassCorrectParameters() {
        // Arrange
        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(validScrapeResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(validScrapeResult.coveredMarkets())).thenReturn(mockMarkets);
        when(productMatchingService.findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class)))
                .thenReturn(mockProductInfo);

        // Act
        productInfoService.processAndSaveScrapeResult(validScrapeResult);

        // Assert - Capture and verify parameters
        ArgumentCaptor<ScrapeResultDto.ScrapedProduct> productCaptor =
                ArgumentCaptor.forClass(ScrapeResultDto.ScrapedProduct.class);
        verify(dailyPriceRecordService, times(3)).createRecordForAllMarkets(
                productCaptor.capture(),
                eq(mockProductInfo),
                eq(mockPriceReport),
                eq(mockMarkets)
        );

        List<ScrapeResultDto.ScrapedProduct> capturedProducts = productCaptor.getAllValues();
        assertEquals(3, capturedProducts.size());
        assertEquals("Bangus", capturedProducts.get(0).commodity());
        assertEquals("Galunggong", capturedProducts.get(1).commodity());
        assertEquals("Kamatis", capturedProducts.get(2).commodity());
    }

    @Test
    @DisplayName("Business Logic: Each product gets matched independently")
    void processAndSaveScrapeResult_MultipleProducts_ShouldMatchEachIndependently() {
        // Arrange
        ProductInfo bangusInfo = new ProductInfo();
        bangusInfo.setId(1L);
        bangusInfo.setProductName("Bangus");

        ProductInfo galunggongInfo = new ProductInfo();
        galunggongInfo.setId(2L);
        galunggongInfo.setProductName("Galunggong");

        ProductInfo kamatisfInfo = new ProductInfo();
        kamatisfInfo.setId(3L);
        kamatisfInfo.setProductName("Kamatis");

        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(validScrapeResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(validScrapeResult.coveredMarkets())).thenReturn(mockMarkets);
        when(productMatchingService.findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class)))
                .thenReturn(bangusInfo, galunggongInfo, kamatisfInfo);

        // Act
        productInfoService.processAndSaveScrapeResult(validScrapeResult);

        // Assert - Verify each product was matched
        verify(productMatchingService, times(3)).findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class));

        // Verify dailyPriceRecordService received different ProductInfo objects
        ArgumentCaptor<ProductInfo> productInfoCaptor = ArgumentCaptor.forClass(ProductInfo.class);
        verify(dailyPriceRecordService, times(3)).createRecordForAllMarkets(
                any(ScrapeResultDto.ScrapedProduct.class),
                productInfoCaptor.capture(),
                eq(mockPriceReport),
                eq(mockMarkets)
        );

        List<ProductInfo> capturedProductInfos = productInfoCaptor.getAllValues();
        assertEquals("Bangus", capturedProductInfos.get(0).getProductName());
        assertEquals("Galunggong", capturedProductInfos.get(1).getProductName());
        assertEquals("Kamatis", capturedProductInfos.get(2).getProductName());
    }

    @Test
    @DisplayName("Real World: Complete market day processing - Balintawak Saturday")
    void processAndSaveScrapeResult_CompleteMarketDay_ShouldProcessAllData() {
        // Arrange - Realistic Saturday market scenario
        List<ScrapeResultDto.ScrapedProduct> saturdayProducts = Arrays.asList(
                new ScrapeResultDto.ScrapedProduct("FISH", "Bangus", "Dagupan", "kg", 180.0),
                new ScrapeResultDto.ScrapedProduct("FISH", "Tilapia", "Taal", "kg", 130.0),
                new ScrapeResultDto.ScrapedProduct("MEAT", "Pork Liempo", "Local", "kg", 300.0),
                new ScrapeResultDto.ScrapedProduct("VEGETABLES", "Sitaw", "Laguna", "kg", 80.0),
                new ScrapeResultDto.ScrapedProduct("VEGETABLES", "Talong", "Batangas", "kg", 70.0)
        );

        ScrapeResultDto saturdayResult = new ScrapeResultDto("success", "2025-12-15", "https://example.com", validScrapeResult.coveredMarkets(), saturdayProducts);

        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(saturdayResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(saturdayResult.coveredMarkets())).thenReturn(mockMarkets);
        when(productMatchingService.findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class)))
                .thenReturn(mockProductInfo);

        // Act
        productInfoService.processAndSaveScrapeResult(saturdayResult);

        // Assert
        verify(productMatchingService, times(5)).findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class));
        verify(dailyPriceRecordService, times(5)).createRecordForAllMarkets(
                any(ScrapeResultDto.ScrapedProduct.class),
                eq(mockProductInfo),
                eq(mockPriceReport),
                eq(mockMarkets)
        );
    }

    @Test
    @DisplayName("Real World: Multiple markets with varying product availability")
    void processAndSaveScrapeResult_MultipleMarkets_ShouldDistributeRecordsCorrectly() {
        // Arrange
        when(priceReportService.reportExists("2025-12-15")).thenReturn(false);
        when(priceReportService.createFromScrapeResult(validScrapeResult)).thenReturn(mockPriceReport);
        when(marketLocationService.findOrCreateMarket(validScrapeResult.coveredMarkets())).thenReturn(mockMarkets);
        when(productMatchingService.findOrCreateProduct(any(ScrapeResultDto.ScrapedProduct.class)))
                .thenReturn(mockProductInfo);

        // Act
        productInfoService.processAndSaveScrapeResult(validScrapeResult);

        // Assert - With 3 products and 3 markets, should create 9 total records (3 x 3)
        verify(dailyPriceRecordService, times(3)).createRecordForAllMarkets(
                any(ScrapeResultDto.ScrapedProduct.class),
                any(ProductInfo.class),
                any(PriceReport.class),
                eq(mockMarkets)
        );
    }
}