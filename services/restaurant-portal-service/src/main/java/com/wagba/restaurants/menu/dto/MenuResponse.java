package com.wagba.restaurants.menu.dto;

import java.util.List;
import java.util.UUID;

public record MenuResponse(
        UUID restaurantId,
        String currency,
        boolean acceptingOrders,
        boolean openNow,
        List<CategoryDto> categories
) {
    public record CategoryDto(UUID id, String name, int sortOrder, List<ItemDto> items) {
    }

    public record ItemDto(UUID id, String name, String description, long priceMinor, String imageUrl,
                          boolean available, List<OptionGroupDto> optionGroups) {
    }

    public record OptionGroupDto(UUID id, String name, int minSelections, int maxSelections,
                                 boolean required, List<OptionDto> options) {
    }

    public record OptionDto(UUID id, String name, long extraPriceMinor) {
    }
}