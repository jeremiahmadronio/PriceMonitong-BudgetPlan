package com.budgetwise.budget.catalog.dto;

import java.time.LocalDateTime;

public record ArchiveTableResponse (
        Long id,
        String productName,
        String category,
        Double lastPrice,
        String unit,
        String origin,
        LocalDateTime archivedDate
) {
}
