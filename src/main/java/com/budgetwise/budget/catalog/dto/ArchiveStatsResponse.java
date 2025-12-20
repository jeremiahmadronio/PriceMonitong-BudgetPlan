package com.budgetwise.budget.catalog.dto;

public record ArchiveStatsResponse(
        long totalArchived,
        long newThisMonth,
        long awaitingReview
) {
}
