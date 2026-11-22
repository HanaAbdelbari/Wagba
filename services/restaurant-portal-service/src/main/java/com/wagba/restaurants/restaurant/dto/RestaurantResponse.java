package com.wagba.restaurants.restaurant.dto;

import com.wagba.restaurants.restaurant.Restaurant;
import com.wagba.restaurants.restaurant.RestaurantStatus;
import com.wagba.restaurants.restaurant.WorkingHours;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The owner's view of a restaurant (includes the review note and visibility). */
public record RestaurantResponse(
        UUID id,
        String name,
        String description,
        String phone,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String timezone,
        String currency,
        RestaurantStatus status,
        boolean acceptingOrders,
        boolean publiclyVisible,
        String reviewNote,
        Instant reviewedAt,
        List<WorkingHoursResponse> workingHours,
        Instant createdAt,
        Instant updatedAt
) {
    public static RestaurantResponse from(Restaurant r, List<WorkingHours> hours) {
        return new RestaurantResponse(r.getId(), r.getName(), r.getDescription(), r.getPhone(), r.getAddress(),
                r.getLatitude(), r.getLongitude(), r.getTimezone(), r.getCurrency(), r.getStatus(),
                r.isAcceptingOrders(), r.isPubliclyVisible(), r.getReviewNote(), r.getReviewedAt(),
                hours.stream().map(WorkingHoursResponse::from).toList(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}