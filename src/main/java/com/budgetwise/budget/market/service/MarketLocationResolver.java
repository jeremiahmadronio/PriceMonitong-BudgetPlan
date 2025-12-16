package com.budgetwise.budget.market.service;

import com.budgetwise.budget.market.entity.MarketLocation;
import com.budgetwise.budget.market.repository.MarketLocationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MarketLocationResolver {

    private final MarketLocationRepository marketLocationRepository;

    public MarketLocationResolver(MarketLocationRepository marketLocationRepository) {
        this.marketLocationRepository = marketLocationRepository;
    }


    /**
     * Efficiently finds or creates multiple markets in a single transaction flow.
     * Uses strict Batch Processing to prevent N+1 Select problems.
     *
     * @param coveredMarkets List of market names strings (e.g., from Scraper)
     * @return A list of persisted MarketLocation entities (both existing and newly created)
     */
    @Transactional
    public List<MarketLocation> findOrCreateMarket(List<String> coveredMarkets ) {

        //  Guard Clause: Handle empty inputs early to save resources
        if(coveredMarkets == null || coveredMarkets.isEmpty()){
            return List.of();
        }

        //  Pre-processing:
        //    - Trim whitespace to avoid dirty data (" Marikina " vs "Marikina")
        //    - Use Set to automatically remove duplicates from the input
        Set<String> uniqueMarkets = coveredMarkets.stream()
                .map(String::trim)
                .collect(Collectors.toSet());

        //  Batch Query (Optimization):
        //    Fetch ALL existing markets in ONE database query using 'IN' clause.
        //    Avoids looping through the database 50 times for 50 markets.
        List<MarketLocation> existingMarkets = marketLocationRepository.findByMarketLocationIn(new ArrayList<>(uniqueMarkets));

        //  Extraction:
        //    Create a reference Set of names that ALREADY exist in the DB.
        Set<String> existingMarketNames = existingMarkets.stream()
                .map(MarketLocation::getMarketLocation)
                .collect(Collectors.toSet());

        //  Filtering (The Logic):
        //    Identify which markets are NEW by checking against the existing set.
        List<MarketLocation> newMarkets = uniqueMarkets.stream()
                .filter(market -> !existingMarketNames.contains(market))
                .map(market -> {
                    MarketLocation newMarketLocation = new MarketLocation();
                    newMarketLocation.setMarketLocation(market);
                    newMarketLocation.setStatus(MarketLocation.Status.ACTIVE);
                    return newMarketLocation;
                })
                .collect(Collectors.toList());

        //  Batch Save:
        //    Save all NEW markets in ONE database transaction.
        if(!newMarkets.isEmpty()){
            List<MarketLocation> savedNewMarkets = marketLocationRepository.saveAll(newMarkets);
            existingMarkets.addAll(savedNewMarkets);

        }
        //  Return the complete list (Old + New) ready for linking
        return existingMarkets;
    }
}
