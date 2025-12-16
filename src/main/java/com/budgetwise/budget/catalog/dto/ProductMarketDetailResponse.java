package com.budgetwise.budget.catalog.dto;


import java.util.List;


public record ProductMarketDetailResponse (
        Long productId,
        String productName,
        List<MarketDetail> marketDetails
){

}