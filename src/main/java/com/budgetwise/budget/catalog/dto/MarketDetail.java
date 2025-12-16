package com.budgetwise.budget.catalog.dto;

import com.budgetwise.budget.market.entity.MarketLocation;

import java.time.LocalDateTime;

public record MarketDetail(
        Long marketId,
        String marketName,
        MarketLocation.Type marketType,
        LocalDateTime marketOpeningTime,
        LocalDateTime marketClosingTime
){
}