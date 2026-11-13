package com.wagba.users.admin.dto;

import com.wagba.users.user.Role;
import com.wagba.users.user.User;
import com.wagba.users.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        Role role,
        UserStatus status,
        Instant emailVerifiedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getStatus(), u.getEmailVerifiedAt(), u.getCreatedAt(), u.getUpdatedAt());
    }
}