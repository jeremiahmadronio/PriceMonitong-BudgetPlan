package com.budgetwise.budget.market.dto;
import com.budgetwise.budget.market.entity.MarketLocation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
public record CreateMarket(


        @NotBlank(message = "Market location name is required")
        String marketLocation,

        @NotNull(message = "Type is required")
        MarketLocation.Type type,

        MarketLocation.Status status,

        @NotNull(message = "Latitude is required")
        Double latitude,

        @NotNull(message = "Longitude is required")
        Double longitude,

        LocalDateTime openingTime,
        LocalDateTime closingTime,

        String description
) {
}
