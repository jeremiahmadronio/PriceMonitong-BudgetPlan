package com.budgetwise.budget.market.service;

import com.budgetwise.budget.market.dto.*;
import com.budgetwise.budget.market.entity.MarketLocation;
import com.budgetwise.budget.market.repository.MarketLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        marketLocation.setUpdatedAt(LocalDateTime.now());
        marketLocationRepository.save(marketLocation);

        return status;



    }


    /**
     * Creates and persists a new Market Location.
     * Performs validation to ensure the market name is unique before saving.
     * Sets default values for Status (ACTIVE), Ratings (0.0), and Audit timestamps.
     *
     * @param request The data transfer object (Record) containing the market details.
     * Must not be null.
     * @return The persisted {@link MarketLocation} entity with generated ID.
     * @throws IllegalArgumentException if a market with the same location name already exists.
     */
    @Transactional
    public MarketLocation addMarket(CreateMarket request) {

        if (marketLocationRepository.existsByMarketLocation(request.marketLocation())) {
            throw new IllegalArgumentException("Market location already exists.");
        }

        MarketLocation market = new MarketLocation();

        market.setMarketLocation(request.marketLocation());
        market.setType(request.type());

        market.setStatus(Optional.ofNullable(request.status())
                .orElse(MarketLocation.Status.ACTIVE));

        market.setLatitude(request.latitude());
        market.setLongitude(request.longitude());
        market.setOpeningTime(request.openingTime());
        market.setClosingTime(request.closingTime());
        market.setDescription(request.description());

        market.setRatings(0.0);
        market.setUpdatedAt(LocalDateTime.now());

        return marketLocationRepository.save(market);
    }


    /**
     * Updates an existing Market Location.
     * * @param id The ID of the market to update.
     * @param request The new data containing updates.
     * @return The updated entity.
     * @throws RuntimeException if the market ID is not found (Change to custom exception later).
     * @throws IllegalArgumentException if the new name is already taken by another market.
     */
    @Transactional
    public MarketLocation updateMarket(Long id, UpdateMarket request) {

        MarketLocation market = marketLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Market not found with id: " + id));


        if (marketLocationRepository.existsByMarketLocationAndIdNot(request.marketLocation(), id)) {
            throw new IllegalArgumentException("Market name already exists on another record.");
        }

        market.setMarketLocation(request.marketLocation());
        market.setType(request.type());
        market.setStatus(request.status());
        market.setLatitude(request.latitude());
        market.setLongitude(request.longitude());
        market.setOpeningTime(request.openingTime());
        market.setClosingTime(request.closingTime());
        market.setDescription(request.description());


        market.setUpdatedAt(LocalDateTime.now());


        return marketLocationRepository.save(market);
    }
}
