package com.budgetwise.budget.catalog.service;

import com.budgetwise.budget.catalog.entity.DailyPriceRecord;
import com.budgetwise.budget.market.entity.MarketLocation;
import com.budgetwise.budget.catalog.entity.PriceReport;
import com.budgetwise.budget.catalog.entity.ProductInfo;
import com.budgetwise.budget.catalog.repository.DailyPriceRecordRepository;
import com.budgetwise.budget.integration.scrapper.dto.ScrapeResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for DailyPriceRecordService
 * Validates price record creation logic, batch processing, market broadcasting, and edge cases
 * Uses realistic DA market data (Bangus prices across markets) for test scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailyPriceRecordService Tests")
class DailyPriceIngestionServiceTest {

    @Mock
    private DailyPriceRecordRepository dailyPriceRecordRepository;

    @InjectMocks
    private DailyPriceIngestionService dailyPriceRecordService;

    private ScrapeResultDto.ScrapedProduct bangusDagupan;
    private ScrapeResultDto.ScrapedProduct tilapiaLocal;
    private ScrapeResultDto.ScrapedProduct galunggong;
    private ProductInfo bangusProduct;
    private ProductInfo tilapiaProduct;
    private ProductInfo galunggongProduct;
    private PriceReport priceReport;
    private MarketLocation marikina;
    private MarketLocation quiapo;
    private MarketLocation divisoria;
    private MarketLocation binondo;
    private List<MarketLocation> threeMarkets;
    private List<MarketLocation> allFourMarkets;

    @BeforeEach
    void setUp() {
        // Test data: Scraped products from DA market
        bangusDagupan = new ScrapeResultDto.ScrapedProduct(
                "FISH",           // category
                "Bangus",         // commodity
                "Dagupan",        // origin
                "kg",             // unit
                180.0             // price
        );

        tilapiaLocal = new ScrapeResultDto.ScrapedProduct(
                "FISH",
                "Tilapia",
                "Local",
                "kg",
                120.0
        );

        galunggong = new ScrapeResultDto.ScrapedProduct(
                "FISH",
                "Galunggong",
                "Navotas",
                "kg",
                220.0
        );

        // Test data: Product entities
        bangusProduct = new ProductInfo();
        bangusProduct.setId(1L);
        bangusProduct.setCategory("FISH");
        bangusProduct.setProductName("Bangus");
        bangusProduct.setStatus(ProductInfo.Status.ACTIVE);

        tilapiaProduct = new ProductInfo();
        tilapiaProduct.setId(2L);
        tilapiaProduct.setCategory("FISH");
        tilapiaProduct.setProductName("Tilapia");
        tilapiaProduct.setStatus(ProductInfo.Status.ACTIVE);

        galunggongProduct = new ProductInfo();
        galunggongProduct.setId(3L);
        galunggongProduct.setCategory("FISH");
        galunggongProduct.setProductName("Galunggong");
        galunggongProduct.setStatus(ProductInfo.Status.ACTIVE);

        // Test data: Price report
        priceReport = new PriceReport();
        priceReport.setId(1L);
        priceReport.setDateProcessed(LocalDateTime.now());
        priceReport.setStatus(PriceReport.Status.COMPLETED);

        // Test data: Market locations
        marikina = new MarketLocation();
        marikina.setId(1L);
        marikina.setMarketLocation("Marikina");

        quiapo = new MarketLocation();
        quiapo.setId(2L);
        quiapo.setMarketLocation("Quiapo");

        divisoria = new MarketLocation();
        divisoria.setId(3L);
        divisoria.setMarketLocation("Divisoria");

        binondo = new MarketLocation();
        binondo.setId(4L);
        binondo.setMarketLocation("Binondo");

        threeMarkets = Arrays.asList(marikina, quiapo, divisoria);
        allFourMarkets = Arrays.asList(marikina, quiapo, divisoria, binondo);
    }

    // ==================== HAPPY PATH ====================

    @Test
    @DisplayName("Happy Path: Single product broadcast to three markets - should create three records")
    void createRecordForAllMarkets_SingleProductThreeMarkets_ShouldCreateThreeRecords() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, threeMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertEquals(3, savedRecords.size());

        for (DailyPriceRecord record : savedRecords) {
            assertEquals(180.0, record.getPrice());
            assertEquals("kg", record.getUnit());
            assertEquals("Dagupan", record.getOrigin());
            assertEquals(bangusProduct, record.getProductInfo());
            assertEquals(priceReport, record.getPriceReport());
        }
    }

    @Test
    @DisplayName("Happy Path: Same price broadcast to all four markets - should create four identical records")
    void createRecordForAllMarkets_SamePriceAllMarkets_ShouldCreateFourIdenticalRecords() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(tilapiaLocal, tilapiaProduct, priceReport, allFourMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertEquals(4, savedRecords.size());

        assertTrue(savedRecords.stream().allMatch(r -> r.getPrice() == 120.0));
        assertTrue(savedRecords.stream().allMatch(r -> "kg".equals(r.getUnit())));
        assertTrue(savedRecords.stream().allMatch(r -> "Local".equals(r.getOrigin())));
        assertTrue(savedRecords.stream().allMatch(r -> tilapiaProduct.equals(r.getProductInfo())));
        assertTrue(savedRecords.stream().allMatch(r -> priceReport.equals(r.getPriceReport())));
    }

    @Test
    @DisplayName("Happy Path: Market location associations - should link each market correctly")
    void createRecordForAllMarkets_MarketAssociation_ShouldLinkMarketsCorrectly() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(galunggong, galunggongProduct, priceReport, threeMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertEquals(3, savedRecords.size());

        assertTrue(savedRecords.stream().anyMatch(r -> marikina.equals(r.getMarketLocation())));
        assertTrue(savedRecords.stream().anyMatch(r -> quiapo.equals(r.getMarketLocation())));
        assertTrue(savedRecords.stream().anyMatch(r -> divisoria.equals(r.getMarketLocation())));
    }

    // ==================== BATCH PROCESSING ====================

    @Test
    @DisplayName("Batch Processing: Should save all records in single transaction")
    void createRecordForAllMarkets_BatchSave_ShouldSaveAllAtOnce() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, allFourMarkets);

        // Assert
        verify(dailyPriceRecordRepository, times(1)).saveAll(any()); // Only ONE save call
    }

    @Test
    @DisplayName("Batch Processing: Large market list - should handle batch efficiently")
    void createRecordForAllMarkets_LargeBatchOfMarkets_ShouldHandleEfficiently() {
        // Arrange
        List<MarketLocation> largeMarketList = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            MarketLocation market = new MarketLocation();
            market.setId((long) i);
            market.setMarketLocation("Market" + i);
            largeMarketList.add(market);
        }

        // Act
        dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, largeMarketList);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository, times(1)).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertEquals(100, savedRecords.size());
    }

    @Test
    @DisplayName("Batch Processing: Should not call saveAll when market list is null")
    void createRecordForAllMarkets_NullMarketList_ShouldNotSave() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, null);

        // Assert
        verify(dailyPriceRecordRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Batch Processing: Should not call saveAll when market list is empty")
    void createRecordForAllMarkets_EmptyMarketList_ShouldNotSave() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, new ArrayList<>());

        // Assert
        verify(dailyPriceRecordRepository, never()).saveAll(any());
    }

    // ==================== PRICE AND UNIT PRESERVATION ====================

    @Test
    @DisplayName("Price Preservation: Different prices per scraped product - should preserve exact price")
    void createRecordForAllMarkets_DifferentPrices_ShouldPreserveExactPrice() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, threeMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertTrue(savedRecords.stream().allMatch(r -> r.getPrice() == 180.0));
    }

    @Test
    @DisplayName("Unit Preservation: Kilogram unit - should preserve exact unit")
    void createRecordForAllMarkets_KilogramUnit_ShouldPreserveUnit() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(tilapiaLocal, tilapiaProduct, priceReport, threeMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertTrue(savedRecords.stream().allMatch(r -> "kg".equals(r.getUnit())));
    }

    @Test
    @DisplayName("Unit Preservation: Different unit types - should handle piece units")
    void createRecordForAllMarkets_PieceUnit_ShouldPreserveUnit() {
        // Arrange
        ScrapeResultDto.ScrapedProduct eggs = new ScrapeResultDto.ScrapedProduct(
                "POULTRY", "Eggs", "Local", "pc", 7.5
        );
        ProductInfo eggProduct = new ProductInfo();
        eggProduct.setId(4L);
        eggProduct.setProductName("Eggs");

        // Act
        dailyPriceRecordService.createRecordForAllMarkets(eggs, eggProduct, priceReport, threeMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertTrue(savedRecords.stream().allMatch(r -> "pc".equals(r.getUnit())));
    }

    @Test
    @DisplayName("Origin Preservation: Specific origin location - should preserve origin correctly")
    void createRecordForAllMarkets_OriginPreservation_ShouldPreserveOrigin() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(galunggong, galunggongProduct, priceReport, threeMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertTrue(savedRecords.stream().allMatch(r -> "Navotas".equals(r.getOrigin())));
    }

    // ==================== ENTITY RELATIONSHIPS ====================

    @Test
    @DisplayName("Entity Relationships: ProductInfo foreign key - should link product to all records")
    void createRecordForAllMarkets_ProductInfoFK_ShouldLinkCorrectly() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, threeMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertTrue(savedRecords.stream().allMatch(r -> bangusProduct.equals(r.getProductInfo())));
        assertTrue(savedRecords.stream().allMatch(r -> r.getProductInfo().getId() == 1L));
    }

    @Test
    @DisplayName("Entity Relationships: PriceReport foreign key - should link report to all records")
    void createRecordForAllMarkets_PriceReportFK_ShouldLinkCorrectly() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(tilapiaLocal, tilapiaProduct, priceReport, allFourMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertTrue(savedRecords.stream().allMatch(r -> priceReport.equals(r.getPriceReport())));
        assertTrue(savedRecords.stream().allMatch(r -> r.getPriceReport().getId() == 1L));
    }

    @Test
    @DisplayName("Entity Relationships: MarketLocation foreign key - should link each unique market")
    void createRecordForAllMarkets_MarketLocationFK_ShouldLinkEachMarket() {
        // Act
        dailyPriceRecordService.createRecordForAllMarkets(galunggong, galunggongProduct, priceReport, threeMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertEquals(3, savedRecords.size());

        List<Long> marketIds = savedRecords.stream()
                .map(r -> r.getMarketLocation().getId())
                .toList();

        assertTrue(marketIds.contains(1L)); // Marikina
        assertTrue(marketIds.contains(2L)); // Quiapo
        assertTrue(marketIds.contains(3L)); // Divisoria
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge Case: Single market - should create exactly one record")
    void createRecordForAllMarkets_SingleMarket_ShouldCreateOneRecord() {
        // Arrange
        List<MarketLocation> singleMarket = Arrays.asList(marikina);

        // Act
        dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, singleMarket);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertEquals(1, savedRecords.size());
        assertEquals(marikina, savedRecords.get(0).getMarketLocation());
    }

    @Test
    @DisplayName("Edge Case: High price value - should handle large decimal prices")
    void createRecordForAllMarkets_HighPrice_ShouldHandleCorrectly() {
        // Arrange
        ScrapeResultDto.ScrapedProduct expensiveProduct = new ScrapeResultDto.ScrapedProduct(
                "MEAT", "Premium Beef", "Imported", "kg", 1500.0
        );
        ProductInfo beefProduct = new ProductInfo();
        beefProduct.setId(5L);
        beefProduct.setProductName("Premium Beef");

        // Act
        dailyPriceRecordService.createRecordForAllMarkets(expensiveProduct, beefProduct, priceReport, threeMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertTrue(savedRecords.stream().allMatch(r -> r.getPrice() == 1500.0));
    }

    @Test
    @DisplayName("Edge Case: Low price value - should handle small decimal prices")
    void createRecordForAllMarkets_LowPrice_ShouldHandleCorrectly() {
        // Arrange
        ScrapeResultDto.ScrapedProduct cheapProduct = new ScrapeResultDto.ScrapedProduct(
                "VEGETABLES", "Kangkong", "Local", "kg", 5.5
        );
        ProductInfo kangkongProduct = new ProductInfo();
        kangkongProduct.setId(6L);
        kangkongProduct.setProductName("Kangkong");

        // Act
        dailyPriceRecordService.createRecordForAllMarkets(cheapProduct, kangkongProduct, priceReport, threeMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertTrue(savedRecords.stream().allMatch(r -> r.getPrice() == 5.5));
    }

    @Test
    @DisplayName("Edge Case: Special character origin - should preserve special characters")
    void createRecordForAllMarkets_SpecialCharacterOrigin_ShouldPreserveCorrectly() {
        // Arrange
        ScrapeResultDto.ScrapedProduct specialOriginProduct = new ScrapeResultDto.ScrapedProduct(
                "FISH", "Bangus", "San Juan-Pasig", "kg", 180.0
        );

        // Act
        dailyPriceRecordService.createRecordForAllMarkets(specialOriginProduct, bangusProduct, priceReport, threeMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertTrue(savedRecords.stream().allMatch(r -> "San Juan-Pasig".equals(r.getOrigin())));
    }

    // ==================== EXCEPTIONS ====================

    @Test
    @DisplayName("Exception: Repository throws exception during saveAll")
    void createRecordForAllMarkets_RepositoryThrowsOnSave_ShouldPropagateException() {
        // Arrange
        when(dailyPriceRecordRepository.saveAll(any()))
                .thenThrow(new RuntimeException("Database save error"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, threeMarkets));
    }

    // ==================== REAL WORLD SCENARIOS ====================

    @Test
    @DisplayName("Real World: DA daily price update - Bangus prices across metro markets")
    void createRecordForAllMarkets_DADailyUpdate_BangusPriceAcrossMetro() {
        // Arrange - Simulating DA daily report: Bangus ₱180/kg across metro markets
        List<MarketLocation> metroMarkets = Arrays.asList(marikina, quiapo, divisoria, binondo);

        // Act
        dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, metroMarkets);

        // Assert
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertEquals(4, savedRecords.size());

        // All records should have same price
        assertTrue(savedRecords.stream().allMatch(r -> r.getPrice() == 180.0));
        // All records should have same product
        assertTrue(savedRecords.stream().allMatch(r -> "Bangus".equals(r.getProductInfo().getProductName())));
        // All records should be from same price report
        assertTrue(savedRecords.stream().allMatch(r -> 1L == r.getPriceReport().getId()));
    }

    @Test
    @DisplayName("Real World: Weekly fish update - Multiple fish products to different markets")
    void createRecordForAllMarkets_WeeklyFishUpdate_MultipleProductsScenario() {
        // First product broadcast
        dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, threeMarkets);
        verify(dailyPriceRecordRepository).saveAll(any());

        // Second product broadcast
        dailyPriceRecordService.createRecordForAllMarkets(tilapiaLocal, tilapiaProduct, priceReport, allFourMarkets);

        // Assert total calls
        verify(dailyPriceRecordRepository, times(2)).saveAll(any());
    }

    @Test
    @DisplayName("Real World: Cross-market price comparison setup - same product across all markets")
    void createRecordForAllMarkets_CrossMarketComparison_SameProductAllMarkets() {
        // Arrange - Setup for price comparison across markets
        List<MarketLocation> allMarkets = new ArrayList<>(allFourMarkets);

        // Act
        dailyPriceRecordService.createRecordForAllMarkets(bangusDagupan, bangusProduct, priceReport, allMarkets);

        // Assert - Each market has exact same price for comparison
        ArgumentCaptor<List<DailyPriceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRecordRepository).saveAll(captor.capture());

        List<DailyPriceRecord> savedRecords = captor.getValue();
        assertEquals(4, savedRecords.size());

        double priceValue = savedRecords.get(0).getPrice();
        assertTrue(savedRecords.stream().allMatch(r -> r.getPrice() == priceValue));
    }
}
