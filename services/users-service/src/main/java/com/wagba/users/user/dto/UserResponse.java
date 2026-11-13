package com.wagba.users.user.dto;

import com.wagba.users.user.Role;
import com.wagba.users.user.User;
import com.wagba.users.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        Role role,
        UserStatus status,
        boolean emailVerified,
        Instant createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getStatus(), u.getEmailVerifiedAt() != null, u.getCreatedAt());
    }
}