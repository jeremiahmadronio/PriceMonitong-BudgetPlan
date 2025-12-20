package com.budgetwise.budget.analytics.service;

import com.budgetwise.budget.analytics.dto.PriceHistoryPoint;
import com.budgetwise.budget.analytics.dto.ProductAnalyticsResponse;
import com.budgetwise.budget.analytics.repository.AnalyticsRepository;
import com.budgetwise.budget.market.entity.MarketLocation;
import com.budgetwise.budget.market.repository.MarketLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor; // <--- ITO ANG KULANG KANINA
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AnalyticsService
 * Validates price history retrieval, statistical aggregation, and volatility logic
 * Uses realistic market data (National vs Specific Market) for test scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Tests")
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository recordRepository;

    @Mock
    private MarketLocationRepository marketRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private MarketLocation mockMarket;
    private List<PriceHistoryPoint> mockHistory;
    private LocalDate testDate;
    private final String PRODUCT_NAME = "Red Onion";
    private final Long MARKET_ID = 1L;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now();

        // Mock Market
        mockMarket = new MarketLocation();
        mockMarket.setId(MARKET_ID);
        mockMarket.setMarketLocation("Pasig Mega Market");

        // Mock History Data
        mockHistory = new ArrayList<>();
        mockHistory.add(new PriceHistoryPoint(testDate.minusDays(2), 200.0));
        mockHistory.add(new PriceHistoryPoint(testDate.minusDays(1), 210.0));
        mockHistory.add(new PriceHistoryPoint(testDate, 205.0));
    }

    // ==================== HAPPY PATH: SPECIFIC MARKET ====================

    @Test
    @DisplayName("Happy Path: Get analytics for a specific market with valid data")
    void getProductAnalytics_SpecificMarket_ShouldReturnCorrectData() {
        // Arrange
        // Mock DB Stats: [Min, Max, Avg]
        List<Object[]> mockStats = Collections.singletonList(new Object[]{180.0, 220.0, 200.0});

        when(marketRepository.findById(MARKET_ID)).thenReturn(Optional.of(mockMarket));
        when(recordRepository.findHistoryByMarket(eq(PRODUCT_NAME), eq(MARKET_ID), any(LocalDate.class)))
                .thenReturn(mockHistory);
        when(recordRepository.findStatsByMarket(eq(PRODUCT_NAME), eq(MARKET_ID), any(LocalDate.class)))
                .thenReturn(mockStats);

        // Act
        ProductAnalyticsResponse response = analyticsService.getProductAnalytics(PRODUCT_NAME, MARKET_ID, 30);

        // Assert
        assertNotNull(response);
        assertEquals(PRODUCT_NAME, response.productName());
        assertEquals("Pasig Mega Market", response.marketName());
        assertEquals(180.0, response.minPrice());
        assertEquals(220.0, response.maxPrice());
        assertEquals(200.0, response.averagePrice()); // 200.0 rounded is 200.0
        assertEquals(3, response.history().size());

        // Volatility Calculation Check: (220 - 180) / 200 * 100 = 20% -> High
        assertEquals("High", response.volatility());

        verify(marketRepository).findById(MARKET_ID);
        verify(recordRepository).findHistoryByMarket(eq(PRODUCT_NAME), eq(MARKET_ID), any(LocalDate.class));
        verify(recordRepository).findStatsByMarket(eq(PRODUCT_NAME), eq(MARKET_ID), any(LocalDate.class));
    }

    // ==================== HAPPY PATH: NATIONAL AVERAGE ====================

    @Test
    @DisplayName("Happy Path: Get analytics for National Average (No Market ID)")
    void getProductAnalytics_NationalAverage_ShouldReturnAggregatedData() {
        // Arrange
        // Mock DB Stats: [Min, Max, Avg]
        List<Object[]> mockStats = Collections.singletonList(new Object[]{190.0, 210.0, 200.0});

        // Use findHistoryNationalAverage instead of findHistoryByMarket
        when(recordRepository.findHistoryNationalAverage(eq(PRODUCT_NAME), any(LocalDate.class)))
                .thenReturn(mockHistory);
        when(recordRepository.findStatsNational(eq(PRODUCT_NAME), any(LocalDate.class)))
                .thenReturn(mockStats);

        // Act - Pass null as marketId
        ProductAnalyticsResponse response = analyticsService.getProductAnalytics(PRODUCT_NAME, null, 30);

        // Assert
        assertNotNull(response);
        assertEquals("National Average", response.marketName());
        assertEquals(190.0, response.minPrice());
        assertEquals(210.0, response.maxPrice());
        assertEquals(200.0, response.averagePrice());

        // Volatility Check: (210 - 190) / 200 * 100 = 10% -> Medium
        assertEquals("Medium", response.volatility());

        verify(marketRepository, never()).findById(anyLong()); // Should NOT check market repo
        verify(recordRepository).findHistoryNationalAverage(eq(PRODUCT_NAME), any(LocalDate.class));
        verify(recordRepository).findStatsNational(eq(PRODUCT_NAME), any(LocalDate.class));
    }

    // ==================== BUSINESS LOGIC: VOLATILITY ====================

    @Test
    @DisplayName("Logic: Low Volatility Calculation (< 5%)")
    void calculateVolatility_LowFluctuation_ShouldReturnLow() {
        // Arrange: Min=100, Max=104, Avg=102 -> Diff=4, %=(4/102)*100 ≈ 3.92%
        List<Object[]> mockStats = Collections.singletonList(new Object[]{100.0, 104.0, 102.0});

        when(recordRepository.findHistoryNationalAverage(anyString(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(recordRepository.findStatsNational(anyString(), any(LocalDate.class)))
                .thenReturn(mockStats);

        // Act
        ProductAnalyticsResponse response = analyticsService.getProductAnalytics(PRODUCT_NAME, null, 30);

        // Assert
        assertEquals("Low", response.volatility());
    }

    @Test
    @DisplayName("Logic: Medium Volatility Calculation (5% - 15%)")
    void calculateVolatility_MediumFluctuation_ShouldReturnMedium() {
        // Arrange: Min=100, Max=110, Avg=105 -> Diff=10, %=(10/105)*100 ≈ 9.5%
        List<Object[]> mockStats = Collections.singletonList(new Object[]{100.0, 110.0, 105.0});

        when(recordRepository.findHistoryNationalAverage(anyString(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(recordRepository.findStatsNational(anyString(), any(LocalDate.class)))
                .thenReturn(mockStats);

        // Act
        ProductAnalyticsResponse response = analyticsService.getProductAnalytics(PRODUCT_NAME, null, 30);

        // Assert
        assertEquals("Medium", response.volatility());
    }

    @Test
    @DisplayName("Logic: High Volatility Calculation (> 15%)")
    void calculateVolatility_HighFluctuation_ShouldReturnHigh() {
        // Arrange: Min=100, Max=150, Avg=125 -> Diff=50, %=(50/125)*100 = 40%
        List<Object[]> mockStats = Collections.singletonList(new Object[]{100.0, 150.0, 125.0});

        when(recordRepository.findHistoryNationalAverage(anyString(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(recordRepository.findStatsNational(anyString(), any(LocalDate.class)))
                .thenReturn(mockStats);

        // Act
        ProductAnalyticsResponse response = analyticsService.getProductAnalytics(PRODUCT_NAME, null, 30);

        // Assert
        assertEquals("High", response.volatility());
    }

    @Test
    @DisplayName("Logic: Zero Average Price (New Product) - Should Default to Low Volatility")
    void calculateVolatility_ZeroAverage_ShouldReturnLow() {
        // Arrange: No data yet
        List<Object[]> mockStats = Collections.singletonList(new Object[]{0.0, 0.0, 0.0});

        when(recordRepository.findHistoryNationalAverage(anyString(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(recordRepository.findStatsNational(anyString(), any(LocalDate.class)))
                .thenReturn(mockStats);

        // Act
        ProductAnalyticsResponse response = analyticsService.getProductAnalytics(PRODUCT_NAME, null, 30);

        // Assert
        assertEquals("Low", response.volatility());
        assertEquals(0.0, response.averagePrice());
    }

    // ==================== BUSINESS LOGIC: DATE HANDLING ====================

    @Test
    @DisplayName("Logic: Verify Date Calculation matches 'days' parameter")
    void getProductAnalytics_DateCalculation_ShouldSubtractCorrectDays() {
        // Arrange
        int daysToLookBack = 7;
        LocalDate expectedStartDate = LocalDate.now().minusDays(daysToLookBack);

        when(recordRepository.findHistoryNationalAverage(anyString(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(recordRepository.findStatsNational(anyString(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Act
        analyticsService.getProductAnalytics(PRODUCT_NAME, null, daysToLookBack);

        // Assert - Verify the date passed to repository is correct
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(recordRepository).findHistoryNationalAverage(eq(PRODUCT_NAME), dateCaptor.capture());

        assertEquals(expectedStartDate, dateCaptor.getValue());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge Case: Market ID provided but Market Not Found")
    void getProductAnalytics_MarketNotFound_ShouldReturnUnknownMarketLabel() {
        // Arrange
        Long nonExistentId = 999L;
        when(marketRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(recordRepository.findHistoryByMarket(anyString(), eq(nonExistentId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(recordRepository.findStatsByMarket(anyString(), eq(nonExistentId), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Act
        ProductAnalyticsResponse response = analyticsService.getProductAnalytics(PRODUCT_NAME, nonExistentId, 30);

        // Assert
        assertEquals("Unknown Market", response.marketName());
        assertEquals(0.0, response.averagePrice()); // Default
    }

    @Test
    @DisplayName("Edge Case: No History Data Available")
    void getProductAnalytics_NoData_ShouldReturnEmptyDefaults() {
        // Arrange
        when(recordRepository.findHistoryNationalAverage(anyString(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        // Empty stats list
        when(recordRepository.findStatsNational(anyString(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Act
        ProductAnalyticsResponse response = analyticsService.getProductAnalytics(PRODUCT_NAME, null, 30);

        // Assert
        assertTrue(response.history().isEmpty());
        assertEquals(0.0, response.minPrice());
        assertEquals(0.0, response.maxPrice());
        assertEquals(0.0, response.averagePrice());
        assertEquals("Low", response.volatility());
    }

    @Test
    @DisplayName("Edge Case: Stats return null values (e.g. products exist but no prices)")
    void getProductAnalytics_NullStats_ShouldHandleGracefully() {
        // Arrange
        // Simulate DB returning [null, null, null]
        List<Object[]> nullStats = Collections.singletonList(new Object[]{null, null, null});

        when(recordRepository.findHistoryNationalAverage(anyString(), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(recordRepository.findStatsNational(anyString(), any(LocalDate.class)))
                .thenReturn(nullStats);

        // Act
        ProductAnalyticsResponse response = analyticsService.getProductAnalytics(PRODUCT_NAME, null, 30);

        // Assert
        assertEquals(0.0, response.minPrice());
        assertEquals(0.0, response.maxPrice());
        assertEquals(0.0, response.averagePrice());
    }
}