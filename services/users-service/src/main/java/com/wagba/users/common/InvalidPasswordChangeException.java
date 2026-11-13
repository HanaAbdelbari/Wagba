package com.wagba.users.common;

public class InvalidPasswordChangeException extends RuntimeException {
    public InvalidPasswordChangeException(String message) {
        super(message);
    }
}