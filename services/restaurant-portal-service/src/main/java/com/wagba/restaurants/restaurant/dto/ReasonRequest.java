package com.wagba.restaurants.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReasonRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 1000, message = "Reason must be at most 1000 characters")
        String reason
) {
}