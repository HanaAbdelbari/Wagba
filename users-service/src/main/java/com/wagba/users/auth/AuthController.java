package com.wagba.users.auth;

import com.wagba.users.auth.dto.*;
import com.wagba.users.common.MessageResponse;
import com.wagba.users.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
@SecurityRequirements   // empty: the endpoints in this controller are public by default
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
    }

    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal Jwt jwt) {
        authService.logoutAll(UUID.fromString(jwt.getSubject()));
    }

    private final AccountRecoveryService recoveryService;

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        recoveryService.forgotPassword(request.email());
        return new MessageResponse("If an account exists for this email, a reset code has been sent");
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        recoveryService.resetPassword(request);
    }

    @PostMapping("/verify-email/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirement(name = "bearer-jwt")
    public MessageResponse requestEmailVerification(@AuthenticationPrincipal Jwt jwt) {
        recoveryService.requestEmailVerification(UUID.fromString(jwt.getSubject()));
        return new MessageResponse("Verification code sent");
    }

    @PostMapping("/verify-email/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearer-jwt")
    public void confirmEmail(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VerifyEmailRequest request) {
        recoveryService.verifyEmail(UUID.fromString(jwt.getSubject()), request.code());
    }
}