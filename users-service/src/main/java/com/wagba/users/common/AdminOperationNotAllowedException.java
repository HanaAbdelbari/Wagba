package com.wagba.users.common;

public class AdminOperationNotAllowedException extends RuntimeException {
    public AdminOperationNotAllowedException(String message) {
        super(message);
    }
}