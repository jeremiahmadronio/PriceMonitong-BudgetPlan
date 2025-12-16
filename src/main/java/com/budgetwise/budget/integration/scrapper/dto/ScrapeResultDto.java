package com.budgetwise.budget.integration.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record ScrapeResultDto(


        String status,

        @JsonProperty("date_processed")
        String dateProcessed,

        @JsonProperty("original_url")
        String url,

        @JsonProperty("covered_markets")
        List<String> coveredMarkets,

        @JsonProperty("price_data")
        List<ScrapedProduct> products

)
{
    public record ScrapedProduct(
            String category,
            String commodity,
            String origin,
            String unit,
            Double price
    ){}




}
