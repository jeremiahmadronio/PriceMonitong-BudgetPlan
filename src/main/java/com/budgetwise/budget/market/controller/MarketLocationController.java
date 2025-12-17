package com.budgetwise.budget.market.controller;

import com.budgetwise.budget.market.dto.*;
import com.budgetwise.budget.market.entity.MarketLocation;
import com.budgetwise.budget.market.service.MarketLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/markets")
@RequiredArgsConstructor
public class MarketLocationController {

    private final MarketLocationService marketLocationService;


    @GetMapping("/stats")
    public ResponseEntity<MarketStatsResponse> getMarketStats() {
        MarketStatsResponse response = marketLocationService.getMarketStats();
        return ResponseEntity.ok(response);
    }


    @GetMapping("/displayMarkets")
    public ResponseEntity<Page<MarketTableResponse>>displayMarkets(
            @PageableDefault(size = 10, sort = "marketLocation", direction = Sort.Direction.ASC) Pageable pageable
    ){
        Page<MarketTableResponse> response = marketLocationService.displayMarketTableInfo(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/marketProducts/{marketId}")
    public ResponseEntity<List<MarketProductsResponse>> displayMarketsProducts(
            @PathVariable("marketId") Long marketId ) {
        List<MarketProductsResponse> response = marketLocationService.displayMarketsProducts(marketId);
        System.out.println("Type ng Count: " + response.get(0).totalProducts().getClass().getName());
        System.out.println("Type ng Price: " + response.get(0).productPrice().getClass().getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/updateStatus")
    public ResponseEntity<UpdateMarketStatus> updateMarketStatus(@RequestBody UpdateMarketStatus status){
        UpdateMarketStatus response = marketLocationService.updateMarketStatus(status);
        return ResponseEntity.ok(response);

    }


    @PostMapping("addMarket")
    public ResponseEntity<MarketLocation> createMarket(
            @RequestBody @Valid CreateMarket request) {

        MarketLocation createdMarket = marketLocationService.addMarket(request);

        // Return 201 Created status + the created object
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdMarket);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MarketLocation> updateMarket(
            @PathVariable Long id,
            @RequestBody @Valid UpdateMarket request) {

        MarketLocation updatedMarket = marketLocationService.updateMarket(id, request);

        return ResponseEntity.ok(updatedMarket);
    }
}
