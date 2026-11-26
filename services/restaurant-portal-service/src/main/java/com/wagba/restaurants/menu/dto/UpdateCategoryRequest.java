package com.wagba.restaurants.menu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PATCH semantics: a null field means "leave unchanged". */
public record UpdateCategoryRequest(

        @Size(max = 100, message = "Name must be at most 100 characters")
        @Pattern(regexp = "^(?!\\s*$).+$", message = "Name must not be blank")
        String name,

        @Min(value = 0, message = "sortOrder must be between 0 and 10000")
        @Max(value = 10000, message = "sortOrder must be between 0 and 10000")
        Integer sortOrder
) {
}