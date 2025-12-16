package com.budgetwise.budget.market.service;

import com.budgetwise.budget.market.dto.MarketProductsResponse;
import com.budgetwise.budget.market.dto.MarketStatsResponse;
import com.budgetwise.budget.market.dto.MarketTableResponse;
import com.budgetwise.budget.market.dto.UpdateMarketStatus;
import com.budgetwise.budget.market.entity.MarketLocation;
import com.budgetwise.budget.market.repository.MarketLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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



    /**
     * Fetches product details for a given market with read-only transaction semantics.
     *
     * @param marketId The ID of the market.
     * @return A list of product projections.
     * @throws IllegalArgumentException if the market does not exist.
     */
    @Transactional(readOnly = true)
    public List<MarketProductsResponse> displayMarketsProducts(Long marketId ) {
        boolean exist = marketLocationRepository.existsById(marketId);
        if(!exist){
            throw new IllegalArgumentException("Market with ID " + marketId + " does not exist.");
        }

        return marketLocationRepository.displayProductByMarketId(marketId);
    }

    /**
     * Updates the status of a market location.
     *
     * @param status DTO containing the market ID and new status.
     * @return The updated UpdateMarketStatus DTO.
     * @throws IllegalArgumentException if the market does not exist.
     */
    @Transactional
    public UpdateMarketStatus updateMarketStatus(UpdateMarketStatus status) {

        MarketLocation marketLocation = marketLocationRepository.findById(status.id())
                .orElseThrow(() -> new IllegalArgumentException("Market with ID " + status.id() + " does not exist."));

        marketLocation.setStatus(status.newStatus());
        marketLocationRepository.save(marketLocation);

        return status;


    }
}
