package com.budgetwise.budget.market.dto;

public record MarketStatsResponse(

        long totalMarkets,
        long activeMarkets,
        long totalSuperMarkets,
        long totalWetMarkets
) {
}
