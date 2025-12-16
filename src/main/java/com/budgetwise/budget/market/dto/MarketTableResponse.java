package com.budgetwise.budget.market.dto;

import com.budgetwise.budget.market.entity.MarketLocation;

public record MarketTableResponse (

        Long id,
        String marketName,
        MarketLocation.Type marketType,
        MarketLocation.Status marketStatus,
        Long totalProductsAvailable

){
}
