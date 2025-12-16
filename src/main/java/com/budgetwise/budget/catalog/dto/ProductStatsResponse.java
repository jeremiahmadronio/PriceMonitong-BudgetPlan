package com.budgetwise.budget.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductStatsResponse {

    private long totalProducts;
    private long activeProducts;
    private long archivedProducts;
    private long totalProductDietaryTags;
}
