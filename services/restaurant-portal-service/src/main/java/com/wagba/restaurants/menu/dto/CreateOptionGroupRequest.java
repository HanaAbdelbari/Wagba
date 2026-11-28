package com.wagba.restaurants.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateOptionGroupRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        /** 0 means optional, 1 or more means the customer must choose. Defaults to 0. */
        @Min(value = 0, message = "minSelections must be between 0 and 30")
        @Max(value = 30, message = "minSelections must be between 0 and 30")
        Integer minSelections,

        /** Defaults to 1 (a single choice, like a size). */
        @Min(value = 1, message = "maxSelections must be between 1 and 30")
        @Max(value = 30, message = "maxSelections must be between 1 and 30")
        Integer maxSelections,

        @Min(value = 0, message = "sortOrder must be between 0 and 10000")
        @Max(value = 10000, message = "sortOrder must be between 0 and 10000")
        Integer sortOrder,

        /** Optional: create the group together with its options. */
        @Size(max = 30, message = "At most 30 options are allowed")
        @Valid
        List<OptionInput> options
) {

    public record OptionInput(

            @NotBlank(message = "Option name is required")
            @Size(max = 100, message = "Option name must be at most 100 characters")
            String name,

            /** Minor units: 2500 means 25.00. Defaults to 0. */
            @PositiveOrZero(message = "Extra price cannot be negative")
            @Max(value = 100_000_000, message = "Extra price is too large")
            Long extraPriceMinor,

            @Min(value = 0, message = "sortOrder must be between 0 and 10000")
            @Max(value = 10000, message = "sortOrder must be between 0 and 10000")
            Integer sortOrder
    ) {
    }
}