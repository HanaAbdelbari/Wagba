package com.wagba.users.auth;

import com.wagba.users.audit.AuditService;
import com.wagba.users.auth.dto.LoginRequest;
import com.wagba.users.auth.dto.RefreshTokenRequest;
import com.wagba.users.auth.dto.RegisterRequest;
import com.wagba.users.auth.dto.TokenResponse;
import com.wagba.users.common.AccountSuspendedException;
import com.wagba.users.common.EmailAlreadyExistsException;
import com.wagba.users.common.InvalidCredentialsException;
import com.wagba.users.common.InvalidRefreshTokenException;
import com.wagba.users.outbox.OutboxService;
import com.wagba.users.security.JwtService;
import com.wagba.users.user.Role;
import com.wagba.users.user.User;
import com.wagba.users.user.UserEventPayloads;
import com.wagba.users.user.UserRepository;
import com.wagba.users.user.UserStatus;
import com.wagba.users.user.dto.UserResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final OutboxService outboxService;

    private String dummyHash;

    @PostConstruct
    void init() {
        // used to keep login timing similar when the email does not exist
        dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.Customer); // always Customer on public registration

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // someone registered the same email at the same moment (the unique index caught it)
            throw new EmailAlreadyExistsException(email);
        }

        // audit row and outbox event are saved in the same transaction as the user
        auditService.record(user.getId(), user.getId(), "USER_CREATED",
                Map.of("role", "Customer", "source", "self-registration"), null);
        outboxService.publish("User", user.getId(), UserEventPayloads.version(user),
                "UserRegistered", UserEventPayloads.registered(user));

        return UserResponse.from(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.getStatus() == UserStatus.Suspended) {
            throw new AccountSuspendedException();
        }

        JwtService.AccessToken access = jwtService.issueAccessToken(user);
        String refresh = refreshTokenService.create(user.getId(), UUID.randomUUID());

        return new TokenResponse(access.value(), "Bearer", access.expiresInSeconds(),
                refresh, UserResponse.from(user));
    }

    // Intentionally NOT @Transactional: the token rotation must commit on its own,
    // even if we throw afterwards (suspended account).
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(request.refreshToken());

        User user = userRepository.findById(rotation.userId())
                .orElseThrow(InvalidRefreshTokenException::new);

        if (user.getStatus() == UserStatus.Suspended) {
            refreshTokenService.revokeAllForUser(user.getId());
            throw new AccountSuspendedException();
        }

        JwtService.AccessToken access = jwtService.issueAccessToken(user);
        return new TokenResponse(access.value(), "Bearer", access.expiresInSeconds(),
                rotation.newRawToken(), UserResponse.from(user));
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeSession(request.refreshToken());
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
        auditService.record(userId, userId, "ALL_SESSIONS_REVOKED", null,
                "User requested logout from all devices");
    }
}