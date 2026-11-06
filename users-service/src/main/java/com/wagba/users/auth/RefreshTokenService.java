package com.wagba.users.auth;

import com.wagba.users.common.InvalidRefreshTokenException;
import com.wagba.users.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties props;

    public record Rotation(UUID userId, String newRawToken) {
    }

    private record Issued(RefreshToken entity, String raw) {
    }

    /** Starts a new session (login). Returns the raw token; only its hash is stored. */
    @Transactional
    public String create(UUID userId, UUID familyId) {
        return issue(userId, familyId).raw();
    }

    /**
     * noRollbackFor is essential: when reuse is detected we revoke the whole family
     * and then throw. Without it the revocation would be rolled back with the exception.
     * as when the transaction got error , it rollbacks
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String raw) {
        RefreshToken old = repository.findByTokenHashForUpdate(hash(raw))
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = Instant.now();

        if (old.getRevokedAt() != null) {
            // An already-used (or logged-out) token came back: treat the session as stolen
            repository.revokeFamily(old.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (old.getExpiresAt().isBefore(now)) {
            throw new InvalidRefreshTokenException();
        }

        Issued next = issue(old.getUserId(), old.getFamilyId());
        old.setRevokedAt(now);
        old.setReplacedById(next.entity().getId());

        return new Rotation(old.getUserId(), next.raw());
    }

    /** Logout of one device. Silent if the token is unknown, so it reveals nothing. */
    @Transactional
    public void revokeSession(String raw) {
        repository.findByTokenHash(hash(raw))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    private Issued issue(UUID userId, UUID familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setFamilyId(familyId);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(props.refreshTokenTtl()));
        repository.save(token);

        return new Issued(token, raw);
    }

    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}