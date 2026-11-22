package com.wagba.restaurants.restaurant;

import java.util.LinkedHashMap;
import java.util.Map;

/** Event payloads hold strings, booleans and numbers only. */
public final class RestaurantEventPayloads {

    private RestaurantEventPayloads() {
    }

    public static int version(Restaurant r) {
        return r.getVersion() == null ? 0 : r.getVersion();
    }

    /** Full snapshot, so consumers (orders, reporting) never need to call back. */
    public static Map<String, Object> snapshot(Restaurant r) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("restaurantId", r.getId().toString());
        p.put("ownerUserId", r.getOwnerUserId().toString());
        p.put("name", r.getName());
        p.put("description", r.getDescription());
        p.put("phone", r.getPhone());
        p.put("address", r.getAddress());
        p.put("latitude", r.getLatitude() == null ? null : r.getLatitude().toPlainString());
        p.put("longitude", r.getLongitude() == null ? null : r.getLongitude().toPlainString());
        p.put("timezone", r.getTimezone());
        p.put("currency", r.getCurrency());
        p.put("status", r.getStatus().name());
        p.put("acceptingOrders", r.isAcceptingOrders());
        return p;
    }

    public static Map<String, Object> availability(Restaurant r) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("restaurantId", r.getId().toString());
        p.put("acceptingOrders", r.isAcceptingOrders());
        p.put("status", r.getStatus().name());
        return p;
    }
}