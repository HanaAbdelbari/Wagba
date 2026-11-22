package com.wagba.restaurants.restaurant;

import com.wagba.restaurants.common.InvalidRequestException;
import com.wagba.restaurants.restaurant.dto.WorkingHoursRequest;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Rejects overlapping periods. Every period is placed on a circular week of 7 * 1440 minutes,
 * so a Friday 18:00 to 02:00 period is compared against Saturday morning too.
 */
public final class WorkingHoursValidator {

    private static final int DAY = 24 * 60;
    private static final int WEEK = 7 * DAY;

    private record Segment(int owner, int start, int end) {
    }

    private WorkingHoursValidator() {
    }

    public static int minutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    /** At or before the opening time means the period ends on the next day. */
    public static boolean closesNextDay(LocalTime opens, LocalTime closes) {
        return minutes(closes) <= minutes(opens);
    }

    public static void validateNoOverlap(List<WorkingHoursRequest.Period> periods) {
        List<Segment> segments = new ArrayList<>();

        for (int i = 0; i < periods.size(); i++) {
            WorkingHoursRequest.Period p = periods.get(i);
            int opens = minutes(p.opensAt());
            int closes = minutes(p.closesAt());
            int duration = closes > opens ? closes - opens : DAY - opens + closes; // opens == closes means 24h
            int start = (p.dayOfWeek() - 1) * DAY + opens;
            int end = start + duration;

            if (end <= WEEK) {
                segments.add(new Segment(i, start, end));
            } else { // wraps from Sunday night to Monday morning
                segments.add(new Segment(i, start, WEEK));
                segments.add(new Segment(i, 0, end - WEEK));
            }
        }

        for (int a = 0; a < segments.size(); a++) {
            for (int b = a + 1; b < segments.size(); b++) {
                Segment x = segments.get(a);
                Segment y = segments.get(b);
                if (x.owner() != y.owner() && x.start() < y.end() && y.start() < x.end()) {
                    DayOfWeek day = DayOfWeek.of(periods.get(y.owner()).dayOfWeek());
                    throw new InvalidRequestException("Working hours overlap around " + day);
                }
            }
        }
    }
}