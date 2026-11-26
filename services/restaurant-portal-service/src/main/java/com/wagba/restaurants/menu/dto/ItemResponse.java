package com.wagba.restaurants.menu.dto;

import com.wagba.restaurants.menu.MenuItem;

import java.time.Instant;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        UUID restaurantId,
        UUID categoryId,
        String name,
        String description,
        long priceMinor,
        String currency,
        String imageUrl,
        int sortOrder,
        boolean available,
        boolean active,
        Integer version,
        Instant createdAt,
        Instant updatedAt
) {
    public static ItemResponse from(MenuItem i, String currency) {
        return new ItemResponse(i.getId(), i.getRestaurantId(), i.getCategoryId(), i.getName(),
                i.getDescription(), i.getPriceMinor(), currency, i.getImageUrl(), i.getSortOrder(),
                i.isAvailable(), i.isActive(), i.getVersion(), i.getCreatedAt(), i.getUpdatedAt());
    }
}