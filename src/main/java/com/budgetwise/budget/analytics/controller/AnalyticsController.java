package com.budgetwise.budget.analytics.controller;

import com.budgetwise.budget.analytics.dto.ProductAnalyticsResponse;
import com.budgetwise.budget.analytics.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * API Endpoint for Price Analytics.
     * * GET /api/v1/analytics/product
     * - If marketId is present: Returns stats for that specific market.
     * - If marketId is missing: Returns the National Average across all markets.
     * * @param productName Exact name of the product (case-sensitive).
     * @param marketId (Optional) ID of the market location.
     * @param days (Optional) Number of days to look back (Default: 30 days).
     */
    @GetMapping("/product")
    public ResponseEntity<ProductAnalyticsResponse> getProductAnalytics(
            @RequestParam("productName") String productName,
            @RequestParam(value = "marketId", required = false) Long marketId,
            @RequestParam(value = "days", defaultValue = "30") int days // Default 30 days
    ) {
        return ResponseEntity.ok(analyticsService.getProductAnalytics(productName, marketId, days));
    }
}