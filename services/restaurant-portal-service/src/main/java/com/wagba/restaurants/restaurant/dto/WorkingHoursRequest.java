package com.wagba.restaurants.restaurant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

/** The whole weekly schedule. An empty list means the restaurant has no opening hours (closed). */
public record WorkingHoursRequest(

        @NotNull(message = "hours is required")
        @Size(max = 28, message = "At most 28 periods are allowed")
        @Valid
        List<Period> hours
) {

    /** closesAt at or before opensAt means the period runs past midnight into the next day. */
    public record Period(
            @NotNull(message = "dayOfWeek is required")
            @Min(value = 1, message = "dayOfWeek must be 1 (Monday) to 7 (Sunday)")
            @Max(value = 7, message = "dayOfWeek must be 1 (Monday) to 7 (Sunday)")
            Integer dayOfWeek,

            @NotNull(message = "opensAt is required") LocalTime opensAt,
            @NotNull(message = "closesAt is required") LocalTime closesAt
    ) {
    }
}