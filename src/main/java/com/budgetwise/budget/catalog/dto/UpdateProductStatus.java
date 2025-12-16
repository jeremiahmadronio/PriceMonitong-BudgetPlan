package com.budgetwise.budget.catalog.dto;

public record UpdateProductStatus (

        Long id,
        String newStatus,
        String message
) {
}
