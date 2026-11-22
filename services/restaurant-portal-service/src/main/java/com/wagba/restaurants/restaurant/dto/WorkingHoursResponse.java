package com.wagba.restaurants.restaurant.dto;

import com.wagba.restaurants.restaurant.WorkingHours;

import java.time.LocalTime;

public record WorkingHoursResponse(int dayOfWeek, LocalTime opensAt, LocalTime closesAt, boolean closesNextDay) {

    public static WorkingHoursResponse from(WorkingHours w) {
        return new WorkingHoursResponse(w.getDayOfWeek(), w.getOpensAt(), w.getClosesAt(), w.isClosesNextDay());
    }
}