package com.budgetwise.budget.analytics.service;

import com.budgetwise.budget.analytics.dto.PriceHistoryPoint;
import com.budgetwise.budget.analytics.dto.ProductAnalyticsResponse;
import com.budgetwise.budget.analytics.repository.AnalyticsRepository;
import com.budgetwise.budget.catalog.repository.DailyPriceRecordRepository;
import com.budgetwise.budget.market.repository.MarketLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnalyticsService {

    private final AnalyticsRepository recordRepository;
    private final MarketLocationRepository marketRepository;

    public AnalyticsService(AnalyticsRepository recordRepository, MarketLocationRepository marketRepository) {
        this.recordRepository = recordRepository;
        this.marketRepository = marketRepository;
    }



    /**
     * Core business logic for analytics.
     * * Logic Flow:
     * 1. Check if marketId is provided.
     * - Yes: Fetch data strictly for that specific market.
     * - No: Aggregate data from ALL markets (National Average).
     * 2. Retrieve Stats (Min, Max, Avg) using optimized DB queries.
     * 3. Compute Volatility (Price fluctuation analysis).
     */
    @Transactional(readOnly = true)
    public ProductAnalyticsResponse getProductAnalytics(String productName, Long marketId, int days) {

        LocalDate startDate = LocalDate.now().minusDays(days);
        List<PriceHistoryPoint> history;
        Double min = 0.0, max = 0.0, avg = 0.0;
        String marketLabel;

        if (marketId != null && marketId > 0) {
            // Specific Market
            marketLabel = marketRepository.findById(marketId)
                    .map(m -> m.getMarketLocation())
                    .orElse("Unknown Market");

            history = recordRepository.findHistoryByMarket(productName, marketId, startDate);

            List<Object[]> stats = recordRepository.findStatsByMarket(productName, marketId, startDate);
            if (!stats.isEmpty() && stats.get(0)[0] != null) {
                min = (Double) stats.get(0)[0];
                max = (Double) stats.get(0)[1];
                avg = (Double) stats.get(0)[2];
            }

        } else {
            marketLabel = "National Average";
            history = recordRepository.findHistoryNationalAverage(productName, startDate);

            List<Object[]> stats = recordRepository.findStatsNational(productName, startDate);
            if (!stats.isEmpty() && stats.get(0)[0] != null) {
                min = (Double) stats.get(0)[0];
                max = (Double) stats.get(0)[1];
                avg = (Double) stats.get(0)[2];
            }
        }

        avg = Math.round(avg * 100.0) / 100.0;

        String volatility = calculateVolatility(min, max, avg);

        return new ProductAnalyticsResponse(
                productName,
                marketLabel,
                min,
                max,
                avg,
                volatility,
                history
        );
    }

    private String calculateVolatility(Double min, Double max, Double avg) {
        if (avg == 0) return "Low";

        // Simple Formula: Percentage difference between Min and Max
        double diff = max - min;
        double percentageFluctuation = (diff / avg) * 100;

        if (percentageFluctuation < 5) return "Low";       // Less than 5% change
        if (percentageFluctuation < 15) return "Medium";   // 5-15% change
        return "High";                                     // >15% change
    }
}