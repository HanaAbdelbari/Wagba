package com.wagba.restaurants.restaurant.dto;

import com.wagba.restaurants.restaurant.Restaurant;
import com.wagba.restaurants.restaurant.RestaurantStatus;
import com.wagba.restaurants.restaurant.WorkingHours;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** What anyone may see. No owner id, no review note. */
public record PublicRestaurantResponse(
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
        boolean openNow,
        boolean orderable,
        List<WorkingHoursResponse> workingHours
) {
    public static PublicRestaurantResponse from(Restaurant r, List<WorkingHours> hours, boolean openNow) {
        return new PublicRestaurantResponse(r.getId(), r.getName(), r.getDescription(), r.getPhone(),
                r.getAddress(), r.getLatitude(), r.getLongitude(), r.getTimezone(), r.getCurrency(),
                r.getStatus(), r.isAcceptingOrders(), openNow,
                r.isOpenForOrders() && openNow,
                hours.stream().map(WorkingHoursResponse::from).toList());
    }
}