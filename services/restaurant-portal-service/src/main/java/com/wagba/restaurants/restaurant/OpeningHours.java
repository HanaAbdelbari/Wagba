package com.wagba.restaurants.restaurant;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalTime;
import java.util.List;

/** Decides whether a restaurant is open at a given instant, in the restaurant's own timezone. */
public final class OpeningHours {

    private OpeningHours() {
    }

    public static boolean isOpenAt(List<WorkingHours> hours, ZoneId zone, Instant instant) {
        if (hours.isEmpty()) {
            return false; // no periods at all: always closed
        }
        ZonedDateTime local = instant.atZone(zone);
        int today = local.getDayOfWeek().getValue();        // 1 = Monday ... 7 = Sunday
        int yesterday = today == 1 ? 7 : today - 1;
        LocalTime time = local.toLocalTime();

        for (WorkingHours w : hours) {
            if (w.getDayOfWeek() == today) {
                if (w.isClosesNextDay()) {
                    if (!time.isBefore(w.getOpensAt())) {
                        return true;                         // from opening time until midnight
                    }
                } else if (!time.isBefore(w.getOpensAt()) && time.isBefore(w.getClosesAt())) {
                    return true;
                }
            }
            // the tail of yesterday's overnight period, e.g. Friday 18:00 to 02:00 is still open Saturday 01:00
            if (w.getDayOfWeek() == yesterday && w.isClosesNextDay() && time.isBefore(w.getClosesAt())) {
                return true;
            }
        }
        return false;
    }
}