package com.wagba.restaurants.common;

/** A business-rule validation that bean validation cannot express (400). */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}