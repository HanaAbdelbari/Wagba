package com.wagba.restaurants.menu.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

/**
 * PATCH semantics: a null field means "leave unchanged"; an empty string clears the description or image.
 * Availability has its own endpoint.
 */
public record UpdateItemRequest(

        UUID categoryId,

        @Size(max = 150, message = "Name must be at most 150 characters")
        @Pattern(regexp = "^(?!\\s*$).+$", message = "Name must not be blank")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @PositiveOrZero(message = "Price cannot be negative")
        @Max(value = 100_000_000, message = "Price is too large")
        Long priceMinor,

        @Size(max = 1000, message = "Image URL must be at most 1000 characters")
        @Pattern(regexp = "^(https?://\\S+)?$", message = "Image URL must start with http:// or https://")
        String imageUrl,

        @Min(value = 0, message = "sortOrder must be between 0 and 10000")
        @Max(value = 10000, message = "sortOrder must be between 0 and 10000")
        Integer sortOrder
) {
}