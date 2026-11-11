package com.wagba.users.auth;

import com.wagba.users.audit.AuditService;
import com.wagba.users.auth.dto.ResetPasswordRequest;
import com.wagba.users.common.EmailAlreadyVerifiedException;
import com.wagba.users.common.InvalidVerificationCodeException;
import com.wagba.users.common.TooManyRequestsException;
import com.wagba.users.common.UserNotFoundException;
import com.wagba.users.outbox.OutboxService;
import com.wagba.users.user.User;
import com.wagba.users.user.UserEventPayloads;
import com.wagba.users.user.UserRepository;
import com.wagba.users.user.UserStatus;
import com.wagba.users.verification.ChallengePurpose;
import com.wagba.users.verification.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private final UserRepository userRepository;
    private final VerificationService verificationService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final OutboxService outboxService;

    @Value("${wagba.verification.code-ttl-minutes:15}")
    private int ttlMinutes;

    // ---------- password reset ----------

    /** Always completes silently: the caller must not learn whether the email exists. */
    @Transactional
    public void forgotPassword(String rawEmail) {
        User user = userRepository.findByEmailIgnoreCase(normalize(rawEmail)).orElse(null);
        if (user == null || user.getStatus() == UserStatus.Suspended) {
            return;
        }
        if (verificationService.isOnCooldown(user.getId(), ChallengePurpose.PasswordReset)) {
            return;
        }

        String code = verificationService.issue(user.getId(), ChallengePurpose.PasswordReset);

        auditService.record(null, user.getId(), "PASSWORD_RESET_REQUESTED", null, null);
        outboxService.publish("User", user.getId(), UserEventPayloads.version(user),
                "PasswordResetRequested", codePayload(user, code));
    }

    @Transactional(noRollbackFor = InvalidVerificationCodeException.class)
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalize(request.email()))
                .orElseThrow(InvalidVerificationCodeException::new);
        if (user.getStatus() == UserStatus.Suspended) {
            throw new InvalidVerificationCodeException();
        }

        verificationService.verifyAndConsume(user.getId(), ChallengePurpose.PasswordReset, request.code());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAllForUser(user.getId()); // every device must log in again

        auditService.record(user.getId(), user.getId(), "PASSWORD_RESET_COMPLETED", null, null);
    }

    // ---------- email verification ----------

    /** Called from register (same transaction) and from the resend endpoint. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void issueEmailVerification(User user) {
        String code = verificationService.issue(user.getId(), ChallengePurpose.EmailVerification);
        outboxService.publish("User", user.getId(), UserEventPayloads.version(user),
                "EmailVerificationRequested", codePayload(user, code));
    }

    @Transactional
    public void requestEmailVerification(UUID userId) {
        User user = find(userId);
        if (user.getEmailVerifiedAt() != null) {
            throw new EmailAlreadyVerifiedException();
        }
        if (verificationService.isOnCooldown(userId, ChallengePurpose.EmailVerification)) {
            throw new TooManyRequestsException("Please wait a minute before requesting another code");
        }
        issueEmailVerification(user);
    }

    @Transactional(noRollbackFor = InvalidVerificationCodeException.class)
    public void verifyEmail(UUID userId, String code) {
        User user = find(userId);
        if (user.getEmailVerifiedAt() != null) {
            return; // already verified: nothing to do
        }

        verificationService.verifyAndConsume(userId, ChallengePurpose.EmailVerification, code);

        user.setEmailVerifiedAt(Instant.now());
        userRepository.saveAndFlush(user); // flush first so the event carries the new version

        auditService.record(userId, userId, "EMAIL_VERIFIED", null, null);
        outboxService.publish("User", userId, UserEventPayloads.version(user), "EmailVerified",
                Map.of("userId", userId.toString(), "email", user.getEmail()));
    }

    // ---------- helpers ----------

    private User find(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }

    /** The code travels in the event so Notifications can send it. It is short-lived and single-use. */
    private Map<String, Object> codePayload(User user, String code) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("userId", user.getId().toString());
        p.put("email", user.getEmail());
        p.put("fullName", user.getFullName());
        p.put("code", code);
        p.put("expiresInMinutes", ttlMinutes);
        return p;
    }
}