package com.wagba.restaurants.restaurant.dto;

import jakarta.validation.constraints.NotNull;

public record AvailabilityRequest(
        @NotNull(message = "acceptingOrders is required") Boolean acceptingOrders
) {
}