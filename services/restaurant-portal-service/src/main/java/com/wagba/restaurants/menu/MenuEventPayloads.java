package com.wagba.restaurants.menu;

import java.util.LinkedHashMap;
import java.util.Map;

/** Event payloads hold strings, booleans and numbers only. */
public final class MenuEventPayloads {

    private MenuEventPayloads() {
    }

    public static int version(MenuItem i) {
        return i.getVersion() == null ? 0 : i.getVersion();
    }

    /** Full snapshot, so consumers (orders, reporting) never need to call back. */
    public static Map<String, Object> snapshot(MenuItem i, String currency) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("itemId", i.getId().toString());
        p.put("restaurantId", i.getRestaurantId().toString());
        p.put("categoryId", i.getCategoryId().toString());
        p.put("name", i.getName());
        p.put("description", i.getDescription());
        p.put("priceMinor", i.getPriceMinor());
        p.put("currency", currency);
        p.put("imageUrl", i.getImageUrl());
        p.put("available", i.isAvailable());
        p.put("active", i.isActive());
        return p;
    }

    //هنا مش محتاجين نبعت كل بيانات المنتج؛ لأن الحدث محدد جدًا: السعر اتغير.
    public static Map<String, Object> priceChanged(MenuItem i, String currency, long oldPriceMinor) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("itemId", i.getId().toString());
        p.put("restaurantId", i.getRestaurantId().toString());
        p.put("currency", currency);
        p.put("oldPriceMinor", oldPriceMinor);
        p.put("newPriceMinor", i.getPriceMinor());
        return p;
    }

    //دي بتتبعت لما حالة توفر المنتج تتغير
    public static Map<String, Object> availability(MenuItem i) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("itemId", i.getId().toString());
        p.put("restaurantId", i.getRestaurantId().toString());
        p.put("available", i.isAvailable());
        return p;
    }
}