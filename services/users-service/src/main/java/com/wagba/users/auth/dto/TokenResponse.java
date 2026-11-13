package com.wagba.users.auth.dto;

import com.wagba.users.user.dto.UserResponse;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        UserResponse user
) {
}