package com.budgetwise.budget.market.service;

import com.budgetwise.budget.market.dto.MarketStatsResponse;
import com.budgetwise.budget.market.dto.MarketTableResponse;
import com.budgetwise.budget.market.repository.MarketLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketLocationService {

    private final MarketLocationRepository marketLocationRepository;


    /**
     * Aggregates key statistics for the market dashboard.
     * Executes separate counts for total, active status, and market types.
     *
     * @return MarketStatsResponse containing current counts of markets by category.
     */
    @Transactional(readOnly = true)
    public MarketStatsResponse getMarketStats() {
        long totalMarkets = marketLocationRepository.count();
        long activeMarkets = marketLocationRepository.countByStatus(com.budgetwise.budget.market.entity.MarketLocation.Status.ACTIVE);
        long totalWetMarkets = marketLocationRepository.countByMarketType(com.budgetwise.budget.market.entity.MarketLocation.Type.WET_MARKET);
        long totalSupermarkets = marketLocationRepository.countByMarketType(com.budgetwise.budget.market.entity.MarketLocation.Type.SUPERMARKET);

        return new MarketStatsResponse(totalMarkets, activeMarkets, totalWetMarkets, totalSupermarkets);

    }

    /**
     * Retrieves a paginated list of markets with their associated product counts.
     * Uses a projection DTO to optimize data fetching performance.
     *
     * @param pageable Pagination and sorting information provided by the controller.
     * @return A Page of MarketTableResponse containing market details and product availability count.
     */
    @Transactional(readOnly = true)
    public Page<MarketTableResponse> displayMarketTableInfo(Pageable pageable) {
        return marketLocationRepository.displayMarketInformation(pageable);
    }
}
