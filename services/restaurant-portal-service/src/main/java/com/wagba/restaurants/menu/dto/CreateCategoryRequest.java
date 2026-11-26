package com.wagba.restaurants.menu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Min(value = 0, message = "sortOrder must be between 0 and 10000")
        @Max(value = 10000, message = "sortOrder must be between 0 and 10000")
        Integer sortOrder
) {
}