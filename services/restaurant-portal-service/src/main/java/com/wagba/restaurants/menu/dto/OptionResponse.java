package com.wagba.restaurants.menu.dto;

import com.wagba.restaurants.menu.MenuOption;

import java.util.UUID;

public record OptionResponse(UUID id, UUID groupId, String name, long extraPriceMinor, int sortOrder, boolean active) {

    public static OptionResponse from(MenuOption o) {
        return new OptionResponse(o.getId(), o.getGroupId(), o.getName(), o.getExtraPriceMinor(),
                o.getSortOrder(), o.isActive());
    }
}