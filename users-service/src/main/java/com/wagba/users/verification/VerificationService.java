package com.wagba.users.verification;

import com.wagba.users.common.InvalidVerificationCodeException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationChallengeRepository repository;

    @Value("${wagba.verification.pepper}")
    private String pepper;

    @Value("${wagba.verification.code-ttl-minutes:15}")
    private int ttlMinutes;

    @Value("${wagba.verification.max-attempts:5}")
    private int maxAttempts;

    @Value("${wagba.verification.cooldown-seconds:60}")
    private int cooldownSeconds;

    /** Creates a new code and cancels any older active one. Returns the raw code (only its HMAC is stored). */
    @Transactional(propagation = Propagation.MANDATORY)
    public String issue(UUID userId, ChallengePurpose purpose) {
        Instant now = Instant.now();
        repository.invalidateActive(userId, purpose, now);

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        VerificationChallenge challenge = new VerificationChallenge();
        challenge.setUserId(userId);
        challenge.setPurpose(purpose);
        challenge.setCodeHash(hash(userId, purpose, code));
        challenge.setExpiresAt(now.plus(Duration.ofMinutes(ttlMinutes)));
        repository.save(challenge);

        return code;
    }

    @Transactional(readOnly = true)
    public boolean isOnCooldown(UUID userId, ChallengePurpose purpose) {
        return repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose)
                .map(c -> c.getCreatedAt().plusSeconds(cooldownSeconds).isAfter(Instant.now()))
                .orElse(false);
    }

    /**
     * noRollbackFor: a wrong guess throws, but the attempts counter must still be saved.
     * Any caller that opens its own transaction needs the same noRollbackFor.
     */
    @Transactional(noRollbackFor = InvalidVerificationCodeException.class)
    public void verifyAndConsume(UUID userId, ChallengePurpose purpose, String code) {
        VerificationChallenge challenge = repository
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(userId, purpose)
                .orElseThrow(InvalidVerificationCodeException::new);

        Instant now = Instant.now();
        if (challenge.getExpiresAt().isBefore(now)) {
            throw new InvalidVerificationCodeException();
        }

        boolean matches = MessageDigest.isEqual( // constant-time comparison
                hash(userId, purpose, code).getBytes(StandardCharsets.UTF_8),
                challenge.getCodeHash().getBytes(StandardCharsets.UTF_8));

        if (!matches) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= maxAttempts) {
                challenge.setConsumedAt(now); // burned: needs a brand new code
            }
            throw new InvalidVerificationCodeException();
        }

        challenge.setConsumedAt(now);
    }

    private String hash(UUID userId, ChallengePurpose purpose, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal((userId + ":" + purpose + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}