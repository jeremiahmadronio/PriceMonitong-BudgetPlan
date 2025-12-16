package com.budgetwise.budget.market.dto;

import com.budgetwise.budget.market.entity.MarketLocation;
import java.time.LocalDate;

public record MarketProductsResponse(
        Long marketId,
        String marketName,
        MarketLocation.Type type,
        Long totalProducts,
        String productName,
        String productCategory,
        Double productPrice,
        LocalDate dateRecorded
) {
}