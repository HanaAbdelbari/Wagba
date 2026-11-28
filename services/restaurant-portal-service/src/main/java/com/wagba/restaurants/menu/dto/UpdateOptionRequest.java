package com.wagba.restaurants.menu.dto;

import jakarta.validation.constraints.*;

/** PATCH semantics: a null field means "leave unchanged". */
public record UpdateOptionRequest(

        @Size(max = 100, message = "Name must be at most 100 characters")
        @Pattern(regexp = "^(?!\\s*$).+$", message = "Name must not be blank")
        String name,

        @PositiveOrZero(message = "Extra price cannot be negative")
        @Max(value = 100_000_000, message = "Extra price is too large")
        Long extraPriceMinor,

        @Min(value = 0, message = "sortOrder must be between 0 and 10000")
        @Max(value = 10000, message = "sortOrder must be between 0 and 10000")
        Integer sortOrder
) {
}