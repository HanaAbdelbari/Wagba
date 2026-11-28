package com.wagba.restaurants.menu.dto;

import jakarta.validation.constraints.*;

public record CreateOptionRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @PositiveOrZero(message = "Extra price cannot be negative")
        @Max(value = 100_000_000, message = "Extra price is too large")
        Long extraPriceMinor,

        @Min(value = 0, message = "sortOrder must be between 0 and 10000")
        @Max(value = 10000, message = "sortOrder must be between 0 and 10000")
        Integer sortOrder
) {
}