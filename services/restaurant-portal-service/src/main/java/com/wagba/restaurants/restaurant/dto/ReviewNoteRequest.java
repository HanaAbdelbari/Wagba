package com.wagba.restaurants.restaurant.dto;

import jakarta.validation.constraints.Size;

public record ReviewNoteRequest(
        @Size(max = 1000, message = "Note must be at most 1000 characters")
        String note
) {
}