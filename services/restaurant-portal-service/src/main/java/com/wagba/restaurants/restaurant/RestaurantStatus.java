package com.wagba.restaurants.restaurant;

/** Names must match the CHECK constraint on restaurants.status. */
public enum RestaurantStatus {
    Pending,
    Approved,
    Rejected,
    Suspended
}