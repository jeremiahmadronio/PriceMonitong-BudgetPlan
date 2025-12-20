package com.budgetwise.budget.analytics.dto;

import java.util.List;

public record ProductAnalyticsResponse(
        String productName,
        String marketName,
        Double minPrice,
        Double maxPrice,
        Double averagePrice,
        String volatility, // "Low", "Medium", "High"
        List<PriceHistoryPoint> history
) {}