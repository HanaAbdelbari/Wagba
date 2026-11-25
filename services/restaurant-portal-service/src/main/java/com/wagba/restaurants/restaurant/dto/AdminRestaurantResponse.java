package com.wagba.restaurants.restaurant.dto;

import com.wagba.restaurants.restaurant.Restaurant;
import com.wagba.restaurants.restaurant.RestaurantStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** The SuperAdmin's view: everything, including the owner and the review trail. */
public record AdminRestaurantResponse(
        UUID id,
        UUID ownerUserId,
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
        boolean ownerSuspended,
        boolean publiclyVisible,
        UUID reviewedByUserId,
        Instant reviewedAt,
        String reviewNote,
        Integer version,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminRestaurantResponse from(Restaurant r) {
        return new AdminRestaurantResponse(r.getId(), r.getOwnerUserId(), r.getName(), r.getDescription(),
                r.getPhone(), r.getAddress(), r.getLatitude(), r.getLongitude(), r.getTimezone(),
                r.getCurrency(), r.getStatus(), r.isAcceptingOrders(), r.isOwnerSuspended(),
                r.isPubliclyVisible(), r.getReviewedByUserId(), r.getReviewedAt(), r.getReviewNote(),
                r.getVersion(), r.getCreatedAt(), r.getUpdatedAt());
    }
}