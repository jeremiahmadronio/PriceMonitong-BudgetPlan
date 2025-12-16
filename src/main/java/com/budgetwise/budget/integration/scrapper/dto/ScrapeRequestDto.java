package com.budgetwise.budget.integration.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScrapeRequestDto (

        @JsonProperty("target_url")
        String url
){}
