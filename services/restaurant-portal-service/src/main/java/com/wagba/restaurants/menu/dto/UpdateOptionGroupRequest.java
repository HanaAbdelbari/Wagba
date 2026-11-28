package com.wagba.restaurants.menu.dto;

import jakarta.validation.constraints.*;

/** PATCH semantics: a null field means "leave unchanged". */
public record UpdateOptionGroupRequest(

        @Size(max = 100, message = "Name must be at most 100 characters")
        @Pattern(regexp = "^(?!\\s*$).+$", message = "Name must not be blank")
        String name,

        @Min(value = 0, message = "minSelections must be between 0 and 30")
        @Max(value = 30, message = "minSelections must be between 0 and 30")
        Integer minSelections,

        @Min(value = 1, message = "maxSelections must be between 1 and 30")
        @Max(value = 30, message = "maxSelections must be between 1 and 30")
        Integer maxSelections,

        @Min(value = 0, message = "sortOrder must be between 0 and 10000")
        @Max(value = 10000, message = "sortOrder must be between 0 and 10000")
        Integer sortOrder
) {
}