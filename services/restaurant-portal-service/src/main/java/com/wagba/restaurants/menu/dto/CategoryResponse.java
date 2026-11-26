package com.wagba.restaurants.menu.dto;

import com.wagba.restaurants.menu.Category;

import java.util.UUID;

public record CategoryResponse(UUID id, UUID restaurantId, String name, int sortOrder, boolean active) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getRestaurantId(), c.getName(), c.getSortOrder(), c.isActive());
    }
}