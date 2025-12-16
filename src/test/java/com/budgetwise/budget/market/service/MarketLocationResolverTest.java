package com.budgetwise.budget.market.service;

import com.budgetwise.budget.market.entity.MarketLocation;
import com.budgetwise.budget.market.repository.MarketLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for MarketLocationService
 * Validates market finding/creation logic, batch processing, deduplication, and edge cases
 * Uses realistic Philippine market names (Marikina, Quiapo, Divisoria, etc.) for test scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MarketLocationService Tests")
class MarketLocationResolverTest {

    @Mock
    private MarketLocationRepository marketLocationRepository;

    @InjectMocks
    private MarketLocationResolver marketLocationService;

    private List<String> validMarkets;
    private List<String> marketWithDuplicates;
    private List<String> marketWithWhitespace;
    private MarketLocation marikina;
    private MarketLocation quiapo;
    private MarketLocation divisoria;

    @BeforeEach
    void setUp() {
        // Test data: Valid markets list
        validMarkets = Arrays.asList("Marikina", "Quiapo", "Divisoria");

        // Test data: Markets with duplicates
        marketWithDuplicates = Arrays.asList("Marikina", "Quiapo", "Marikina", "Divisoria", "Quiapo");

        // Test data: Markets with whitespace
        marketWithWhitespace = Arrays.asList(" Marikina ", "Quiapo", " Divisoria ", "  Binondo  ");

        // Test data: Existing market entities
        marikina = new MarketLocation();
        marikina.setId(1L);
        marikina.setMarketLocation("Marikina");

        quiapo = new MarketLocation();
        quiapo.setId(2L);
        quiapo.setMarketLocation("Quiapo");

        divisoria = new MarketLocation();
        divisoria.setId(3L);
        divisoria.setMarketLocation("Divisoria");
    }

    // ==================== HAPPY PATH ====================

    @Test
    @DisplayName("Happy Path: All markets exist in database - should return all existing markets")
    void findOrCreateMarket_AllMarketsExist_ShouldReturnAllExisting() {
        // Arrange
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(Arrays.asList(marikina, quiapo, divisoria));

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(validMarkets);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(marikina));
        assertTrue(result.contains(quiapo));
        assertTrue(result.contains(divisoria));
        verify(marketLocationRepository).findByMarketLocationIn(any());
        verify(marketLocationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Happy Path: No markets exist in database - should create all new markets")
    void findOrCreateMarket_NoMarketsExist_ShouldCreateAllNew() {
        // Arrange
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>());
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> {
                    List<MarketLocation> markets = invocation.getArgument(0);
                    List<MarketLocation> saved = new ArrayList<>();
                    long id = 1L;
                    for (MarketLocation market : markets) {
                        market.setId(id++);
                        saved.add(market);
                    }
                    return saved;
                });

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(validMarkets);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(marketLocationRepository).findByMarketLocationIn(any());

        ArgumentCaptor<List<MarketLocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(marketLocationRepository).saveAll(captor.capture());
        List<MarketLocation> savedMarkets = captor.getValue();
        assertEquals(3, savedMarkets.size());
        assertTrue(savedMarkets.stream().anyMatch(m -> "Marikina".equals(m.getMarketLocation())));
        assertTrue(savedMarkets.stream().anyMatch(m -> "Quiapo".equals(m.getMarketLocation())));
        assertTrue(savedMarkets.stream().anyMatch(m -> "Divisoria".equals(m.getMarketLocation())));
    }

    @Test
    @DisplayName("Happy Path: Mix of existing and new markets - should return combined list")
    void findOrCreateMarket_MixedExistingAndNew_ShouldReturnCombined() {
        // Arrange
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(marikina, quiapo)));
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> {
                    List<MarketLocation> markets = invocation.getArgument(0);
                    MarketLocation binondo = new MarketLocation();
                    binondo.setId(4L);
                    binondo.setMarketLocation("Binondo");
                    return Arrays.asList(binondo);
                });

        List<String> markets = Arrays.asList("Marikina", "Quiapo", "Binondo");

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(markets);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(marketLocationRepository).findByMarketLocationIn(any());
        verify(marketLocationRepository).saveAll(any());
    }


    // ==================== DEDUPLICATION ====================

    @Test
    @DisplayName("Deduplication: Duplicate market names - should process only unique markets")
    void findOrCreateMarket_DuplicateMarketNames_ShouldDeduplicate() {
        // Arrange
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>());
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(marketWithDuplicates);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size()); // Only 3 unique markets despite 5 items in input

        ArgumentCaptor<List<MarketLocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(marketLocationRepository).findByMarketLocationIn(any());
        verify(marketLocationRepository).saveAll(captor.capture());

        List<MarketLocation> savedMarkets = captor.getValue();
        assertEquals(3, savedMarkets.size()); // Should save only 3 unique markets
    }

    @Test
    @DisplayName("Deduplication: Input with all same market - should save only once")
    void findOrCreateMarket_AllSameMarketName_ShouldSaveOnce() {
        // Arrange
        List<String> samemarkets = Arrays.asList("Marikina", "Marikina", "Marikina", "Marikina");
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>());
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(samemarkets);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Marikina", result.get(0).getMarketLocation());

        ArgumentCaptor<List<MarketLocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(marketLocationRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    // ==================== WHITESPACE HANDLING ====================

    @Test
    @DisplayName("Whitespace Handling: Markets with leading/trailing spaces - should trim before processing")
    void findOrCreateMarket_MarketsWithWhitespace_ShouldTrimCorrectly() {
        // Arrange
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>());
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(marketWithWhitespace);

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size()); // Marikina, Quiapo, Divisoria, Binondo

        ArgumentCaptor<List<MarketLocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(marketLocationRepository).findByMarketLocationIn(any());
        verify(marketLocationRepository).saveAll(captor.capture());

        List<MarketLocation> savedMarkets = captor.getValue();
        assertTrue(savedMarkets.stream().allMatch(m -> !m.getMarketLocation().startsWith(" ")));
        assertTrue(savedMarkets.stream().allMatch(m -> !m.getMarketLocation().endsWith(" ")));
    }

    @Test
    @DisplayName("Whitespace Handling: Mixed duplicates and whitespace - should deduplicate after trimming")
    void findOrCreateMarket_DuplicatesAndWhitespace_ShouldHandleCorrectly() {
        // Arrange
        List<String> mixed = Arrays.asList(" Marikina ", "Marikina", "  Marikina  ", " Quiapo");
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>());
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(mixed);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // Only 2 unique: Marikina and Quiapo
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Edge Case: Null market list - should return empty list")
    void findOrCreateMarket_NullList_ShouldReturnEmpty() {
        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(null);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(marketLocationRepository, never()).findByMarketLocationIn(any());
        verify(marketLocationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Edge Case: Empty market list - should return empty list")
    void findOrCreateMarket_EmptyList_ShouldReturnEmpty() {
        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(new ArrayList<>());

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(marketLocationRepository, never()).findByMarketLocationIn(any());
        verify(marketLocationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Edge Case: Single market item - should process correctly")
    void findOrCreateMarket_SingleMarket_ShouldProcessCorrectly() {
        // Arrange
        List<String> singleMarket = Arrays.asList("Marikina");
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(Arrays.asList(marikina));

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(singleMarket);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Marikina", result.get(0).getMarketLocation());
        verify(marketLocationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Edge Case: Large batch of markets - should handle batch processing efficiently")
    void findOrCreateMarket_LargeBatchOfMarkets_ShouldHandleEfficiently() {
        // Arrange
        List<String> largeMarketList = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            largeMarketList.add("Market" + i);
        }

        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>());
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(largeMarketList);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.size());
        verify(marketLocationRepository, times(1)).findByMarketLocationIn(any()); // Only ONE query
        verify(marketLocationRepository, times(1)).saveAll(any()); // Only ONE save
    }

    @Test
    @DisplayName("Edge Case: Markets with special characters - should preserve special characters")
    void findOrCreateMarket_MarketsWithSpecialCharacters_ShouldPreserveCharacters() {
        // Arrange
        List<String> specialMarkets = Arrays.asList("M.M. Market", "San Juan-Pasig", "Makati #1");
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>());
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(specialMarkets);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());

        ArgumentCaptor<List<MarketLocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(marketLocationRepository).saveAll(captor.capture());
        List<MarketLocation> savedMarkets = captor.getValue();

        assertTrue(savedMarkets.stream().anyMatch(m -> "M.M. Market".equals(m.getMarketLocation())));
        assertTrue(savedMarkets.stream().anyMatch(m -> "San Juan-Pasig".equals(m.getMarketLocation())));
        assertTrue(savedMarkets.stream().anyMatch(m -> "Makati #1".equals(m.getMarketLocation())));
    }

    @Test
    @DisplayName("Edge Case: Case-sensitive market comparison - should treat same name different cases as different")
    void findOrCreateMarket_DifferentCaseMarkets_ShouldTreatAsSeparate() {
        // Arrange
        List<String> differentCaseMarkets = Arrays.asList("marikina", "MARIKINA", "Marikina");
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>());
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(differentCaseMarkets);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size()); // Should treat as 3 different markets
    }

    // ==================== BATCH PROCESSING ====================

    @Test
    @DisplayName("Batch Processing: Should query database only once for finding existing markets")
    void findOrCreateMarket_BatchQuery_ShouldQueryDatabaseOnce() {
        // Arrange
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(Arrays.asList(marikina));

        // Act
        marketLocationService.findOrCreateMarket(validMarkets);

        // Assert - Verify findByMarketLocationIn called exactly once
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(marketLocationRepository, times(1)).findByMarketLocationIn(captor.capture());

        // Verify it received all unique market names
        List<String> queriedMarkets = captor.getValue();
        assertEquals(3, queriedMarkets.size());
    }

    @Test
    @DisplayName("Batch Processing: Should save all new markets in single transaction")
    void findOrCreateMarket_BatchSave_ShouldSaveAllNewMarketsOnce() {
        // Arrange
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>());
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        marketLocationService.findOrCreateMarket(validMarkets);

        // Assert - Verify saveAll called exactly once
        ArgumentCaptor<List<MarketLocation>> captor = ArgumentCaptor.forClass(List.class);
        verify(marketLocationRepository, times(1)).saveAll(captor.capture());

        List<MarketLocation> savedMarkets = captor.getValue();
        assertEquals(3, savedMarkets.size());
    }

    @Test
    @DisplayName("Batch Processing: Should not call saveAll when all markets already exist")
    void findOrCreateMarket_AllExistBatchSave_ShouldNotSave() {
        // Arrange
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(Arrays.asList(marikina, quiapo, divisoria));

        // Act
        marketLocationService.findOrCreateMarket(validMarkets);

        // Assert
        verify(marketLocationRepository, never()).saveAll(any());
    }

    // ==================== EXCEPTIONS ====================

    @Test
    @DisplayName("Exception: Repository throws exception during findByMarketLocationIn")
    void findOrCreateMarket_RepositoryThrowsOnFind_ShouldPropagateException() {
        // Arrange
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> marketLocationService.findOrCreateMarket(validMarkets));
        verify(marketLocationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Exception: Repository throws exception during saveAll")
    void findOrCreateMarket_RepositoryThrowsOnSave_ShouldPropagateException() {
        // Arrange
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>());
        when(marketLocationRepository.saveAll(any()))
                .thenThrow(new RuntimeException("Database save error"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> marketLocationService.findOrCreateMarket(validMarkets));
    }

    // ==================== REAL WORLD SCENARIOS ====================

    @Test
    @DisplayName("Real World: Weekly market update from scraper - multiple sources with duplicates")
    void findOrCreateMarket_WeeklyScraperUpdate_ShouldHandleDuplicatesAndNewMarkets() {
        // Arrange - Simulating scraper data with duplicates and whitespace
        List<String> scraperData = Arrays.asList(
                "Marikina", " Marikina ", "Marikina",
                "Quiapo", "Quiapo Market",
                " Divisoria ", "Divisoria",
                "Binondo", "  Binondo  ",
                "Salcedo"
        );

        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(marikina, quiapo))); // Wrap in ArrayList for mutability
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> {
                    List<MarketLocation> markets = invocation.getArgument(0);
                    long id = 4L;
                    for (MarketLocation market : markets) {
                        market.setId(id++);
                    }
                    return markets;
                });

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(scraperData);

        // Assert
        assertNotNull(result);
        assertTrue(result.size() >= 5); // At least: Marikina, Quiapo, Quiapo Market, Divisoria, Binondo, Salcedo
        verify(marketLocationRepository).findByMarketLocationIn(any());
        verify(marketLocationRepository).saveAll(any());
    }


    @Test
    @DisplayName("Real World: Same market list processed twice - should reuse existing on second call")
    void findOrCreateMarket_ReprocessSameMarkets_ShouldReuseExisting() {
        // First call: All markets are new
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>())
                .thenReturn(Arrays.asList(marikina, quiapo, divisoria)); // Second call returns all as existing

        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> {
                    List<MarketLocation> markets = invocation.getArgument(0);
                    long id = 1L;
                    for (MarketLocation market : markets) {
                        market.setId(id++);
                    }
                    return markets;
                });

        // Act - First call
        List<MarketLocation> firstResult = marketLocationService.findOrCreateMarket(validMarkets);
        assertEquals(3, firstResult.size());
        verify(marketLocationRepository).saveAll(any());

        // Reset mocks for second call
        reset(marketLocationRepository);
        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(Arrays.asList(marikina, quiapo, divisoria));

        // Act - Second call
        List<MarketLocation> secondResult = marketLocationService.findOrCreateMarket(validMarkets);

        // Assert
        assertEquals(3, secondResult.size());
        verify(marketLocationRepository, never()).saveAll(any()); // Should NOT save on second call
    }

    @Test
    @DisplayName("Real World: Mixed Philippine market sources - national markets and local markets")
    void findOrCreateMarket_PhilippineMarkets_ShouldHandleNationalAndLocal() {
        // Arrange - Mix of national and local Philippine markets
        List<String> philippineMarkets = Arrays.asList(
                "Marikina Fruit and Vegetable Market",
                "Quezon City Proper Market",
                "Divisoria",
                "Balintawak Market",
                "Pasig Riverside",
                "Cabanatuan",
                "Zamboanga City Public Market"
        );

        when(marketLocationRepository.findByMarketLocationIn(any()))
                .thenReturn(new ArrayList<>());
        when(marketLocationRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<MarketLocation> result = marketLocationService.findOrCreateMarket(philippineMarkets);

        // Assert
        assertNotNull(result);
        assertEquals(7, result.size());
        assertTrue(result.stream().anyMatch(m -> "Marikina Fruit and Vegetable Market".equals(m.getMarketLocation())));
        assertTrue(result.stream().anyMatch(m -> "Zamboanga City Public Market".equals(m.getMarketLocation())));
    }
}
