package com.wagba.users.common;

public class AccountSuspendedException extends RuntimeException {
    public AccountSuspendedException() {
        super("This account is suspended");
    }
}