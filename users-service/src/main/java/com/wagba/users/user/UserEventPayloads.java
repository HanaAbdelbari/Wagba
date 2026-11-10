package com.wagba.users.user;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Payloads contain strings only, never passwords or hashes. */
public final class UserEventPayloads {

    private UserEventPayloads() {
    }

    public static int version(User u) {
        return u.getVersion() == null ? 0 : u.getVersion();
    }

    public static Map<String, Object> registered(User u) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("userId", u.getId().toString());
        p.put("email", u.getEmail());
        p.put("fullName", u.getFullName());
        if (u.getPhone() != null) {
            p.put("phone", u.getPhone());
        }
        p.put("role", u.getRole().name());
        return p;
    }

    public static Map<String, Object> statusChanged(User u, UserStatus before, UUID changedBy) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("userId", u.getId().toString());
        p.put("previousStatus", before.name());
        p.put("status", u.getStatus().name());
        p.put("changedBy", changedBy.toString());
        return p;
    }

    public static Map<String, Object> roleChanged(User u, Role before, UUID changedBy) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("userId", u.getId().toString());
        p.put("previousRole", before.name());
        p.put("role", u.getRole().name());
        p.put("changedBy", changedBy.toString());
        return p;
    }
}