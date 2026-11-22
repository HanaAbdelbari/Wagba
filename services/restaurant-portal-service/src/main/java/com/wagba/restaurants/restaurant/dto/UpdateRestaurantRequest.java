package com.wagba.restaurants.restaurant.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * PATCH semantics: a null field means "leave unchanged".
 * Currency, status and owner are deliberately not here.
 */
public record UpdateRestaurantRequest(

        @Size(max = 150, message = "Name must be at most 150 characters")
        @Pattern(regexp = "^(?!\\s*$).+$", message = "Name must not be blank")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Pattern(regexp = "^\\+?[0-9]{7,20}$", message = "Phone number is not valid")
        String phone,

        @Size(max = 500, message = "Address must be at most 500 characters")
        @Pattern(regexp = "^(?!\\s*$).+$", message = "Address must not be blank")
        String address,

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        @Digits(integer = 3, fraction = 6, message = "Latitude allows at most 6 decimal places")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        @Digits(integer = 3, fraction = 6, message = "Longitude allows at most 6 decimal places")
        BigDecimal longitude,

        @Size(max = 60) String timezone
) {
}