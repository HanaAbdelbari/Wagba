package com.wagba.restaurants.menu.dto;

import jakarta.validation.constraints.NotNull;

public record ItemAvailabilityRequest(
        @NotNull(message = "available is required") Boolean available
) {
}