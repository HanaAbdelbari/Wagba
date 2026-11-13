package com.wagba.users.auth;

import com.wagba.users.common.InvalidRefreshTokenException;
import com.wagba.users.security.JwtProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String RAW = "raw-refresh-token";

    @Mock
    RefreshTokenRepository repository;

    RefreshTokenService service;
    final UUID userId = UUID.randomUUID();
    final UUID familyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties("test", "p", "p",
                Duration.ofMinutes(15), Duration.ofDays(14), true);
        service = new RefreshTokenService(repository, props);
    }

    private RefreshToken storedToken(Instant expiresAt, Instant revokedAt) {
        RefreshToken t = new RefreshToken();
        t.setUserId(userId);
        t.setFamilyId(familyId);
        t.setTokenHash(RefreshTokenService.hash(RAW));
        t.setExpiresAt(expiresAt);
        t.setRevokedAt(revokedAt);
        return t;
    }

    private void stubLookup(RefreshToken token) {
        Mockito.when(repository.findByTokenHashForUpdate(RefreshTokenService.hash(RAW)))
                .thenReturn(Optional.ofNullable(token));
    }

    @Test
    void rotate_validToken_revokesOld_andIssuesNewOneInTheSameFamily() {
        RefreshToken old = storedToken(Instant.now().plusSeconds(3600), null);
        stubLookup(old);
        Mockito.when(repository.save(ArgumentMatchers.any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshTokenService.Rotation rotation = service.rotate(RAW);

        Assertions.assertThat(old.getRevokedAt()).isNotNull();
        Assertions.assertThat(rotation.userId()).isEqualTo(userId);
        Assertions.assertThat(rotation.newRawToken()).isNotEqualTo(RAW);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        Mockito.verify(repository).save(saved.capture());
        Assertions.assertThat(saved.getValue().getFamilyId()).isEqualTo(familyId);
        Assertions.assertThat(saved.getValue().getTokenHash()).isNotEqualTo(rotation.newRawToken());
    }

    @Test
    void rotate_reusedToken_revokesTheWholeFamily_andFails() {
        stubLookup(storedToken(Instant.now().plusSeconds(3600), Instant.now().minusSeconds(10)));

        Assertions.assertThatThrownBy(() -> service.rotate(RAW)).isInstanceOf(InvalidRefreshTokenException.class);

        Mockito.verify(repository).revokeFamily(ArgumentMatchers.eq(familyId), ArgumentMatchers.any(Instant.class));
        Mockito.verify(repository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    void rotate_expiredToken_fails_withoutTouchingTheFamily() {
        stubLookup(storedToken(Instant.now().minusSeconds(1), null));

        Assertions.assertThatThrownBy(() -> service.rotate(RAW)).isInstanceOf(InvalidRefreshTokenException.class);

        Mockito.verify(repository, Mockito.never()).revokeFamily(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(repository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    void rotate_unknownToken_fails() {
        stubLookup(null);

        Assertions.assertThatThrownBy(() -> service.rotate(RAW)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void hash_isDeterministic_andDoesNotExposeTheToken() {
        String h1 = RefreshTokenService.hash(RAW);
        String h2 = RefreshTokenService.hash(RAW);

        Assertions.assertThat(h1).isEqualTo(h2).hasSize(64).doesNotContain(RAW);
    }
}