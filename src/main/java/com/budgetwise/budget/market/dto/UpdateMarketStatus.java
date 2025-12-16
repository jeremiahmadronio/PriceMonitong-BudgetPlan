package com.budgetwise.budget.market.dto;

import com.budgetwise.budget.market.entity.MarketLocation;

public record UpdateMarketStatus (

        Long id,
        MarketLocation.Status newStatus

) {
}
