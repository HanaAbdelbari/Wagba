package com.wagba.restaurants.common;

/** The restaurant's current state does not allow the operation (409). */
public class RestaurantStateException extends RuntimeException {
    public RestaurantStateException(String message) {
        super(message);
    }
}