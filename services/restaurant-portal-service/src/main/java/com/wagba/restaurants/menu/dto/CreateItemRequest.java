package com.wagba.restaurants.menu.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CreateItemRequest(

        @NotNull(message = "categoryId is required")
        UUID categoryId,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        /** Minor units: 15000 means 150.00. The currency comes from the restaurant. */
        @NotNull(message = "priceMinor is required")
        @PositiveOrZero(message = "Price cannot be negative")
        @Max(value = 100_000_000, message = "Price is too large")
        Long priceMinor,

        @Size(max = 1000, message = "Image URL must be at most 1000 characters")
        @Pattern(regexp = "^(https?://\\S+)?$", message = "Image URL must start with http:// or https://")
        String imageUrl,

        @Min(value = 0, message = "sortOrder must be between 0 and 10000")
        @Max(value = 10000, message = "sortOrder must be between 0 and 10000")
        Integer sortOrder,

        /** Optional, defaults to true. */
        Boolean available
) {
}