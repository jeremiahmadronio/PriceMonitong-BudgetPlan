package com.budgetwise.budget.analytics.dto;

import java.time.LocalDate;

public record PriceHistoryPoint(
        LocalDate date,
        Double price
) {}