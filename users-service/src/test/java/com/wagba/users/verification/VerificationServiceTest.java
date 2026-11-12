package com.wagba.users.verification;

import com.wagba.users.common.InvalidVerificationCodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    private static final ChallengePurpose PURPOSE = ChallengePurpose.PasswordReset;

    @Mock
    VerificationChallengeRepository repository;

    VerificationService service;
    final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new VerificationService(repository);
        ReflectionTestUtils.setField(service, "pepper", "unit-test-pepper");
        ReflectionTestUtils.setField(service, "ttlMinutes", 15);
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "cooldownSeconds", 60);
    }

    record Issued(String code, VerificationChallenge challenge) {
    }

    private Issued issue() {
        String code = service.issue(userId, PURPOSE);
        ArgumentCaptor<VerificationChallenge> captor = ArgumentCaptor.forClass(VerificationChallenge.class);
        verify(repository).save(captor.capture());
        return new Issued(code, captor.getValue());
    }

    private void stubActive(VerificationChallenge challenge) {
        when(repository.findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(userId, PURPOSE))
                .thenReturn(Optional.of(challenge));
    }

    private static String wrongCodeFor(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    @Test
    void issue_returnsSixDigitCode_andStoresOnlyAHash() {
        Issued issued = issue();

        assertThat(issued.code()).matches("\\d{6}");
        assertThat(issued.challenge().getCodeHash()).doesNotContain(issued.code());
        assertThat(issued.challenge().getExpiresAt()).isAfter(Instant.now());
        assertThat(issued.challenge().getAttempts()).isZero();
    }

    @Test
    void verify_correctCode_consumesTheChallenge() {
        Issued issued = issue();
        stubActive(issued.challenge());

        service.verifyAndConsume(userId, PURPOSE, issued.code());

        assertThat(issued.challenge().getConsumedAt()).isNotNull();
    }

    @Test
    void verify_wrongCode_countsAttempt_andKeepsChallengeAlive() {
        Issued issued = issue();
        stubActive(issued.challenge());

        assertThatThrownBy(() -> service.verifyAndConsume(userId, PURPOSE, wrongCodeFor(issued.code())))
                .isInstanceOf(InvalidVerificationCodeException.class);

        assertThat(issued.challenge().getAttempts()).isEqualTo(1);
        assertThat(issued.challenge().getConsumedAt()).isNull();
    }

    @Test
    void verify_burnsTheChallenge_afterMaxAttempts() {
        Issued issued = issue();
        stubActive(issued.challenge());
        String wrong = wrongCodeFor(issued.code());

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.verifyAndConsume(userId, PURPOSE, wrong))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }

        assertThat(issued.challenge().getAttempts()).isEqualTo(3);
        assertThat(issued.challenge().getConsumedAt()).isNotNull();
    }

    @Test
    void verify_expiredCode_isRejected_evenIfCorrect() {
        Issued issued = issue();
        issued.challenge().setExpiresAt(Instant.now().minusSeconds(1));
        stubActive(issued.challenge());

        assertThatThrownBy(() -> service.verifyAndConsume(userId, PURPOSE, issued.code()))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    @Test
    void verify_withoutActiveChallenge_isRejected() {
        when(repository.findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(userId, PURPOSE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyAndConsume(userId, PURPOSE, "123456"))
                .isInstanceOf(InvalidVerificationCodeException.class);
    }

    @Test
    void cooldown_isActive_rightAfterIssuing_andOverAfterTheWindow() {
        VerificationChallenge recent = new VerificationChallenge();
        recent.setCreatedAt(Instant.now());
        when(repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, PURPOSE))
                .thenReturn(Optional.of(recent));
        assertThat(service.isOnCooldown(userId, PURPOSE)).isTrue();

        recent.setCreatedAt(Instant.now().minusSeconds(120));
        assertThat(service.isOnCooldown(userId, PURPOSE)).isFalse();
    }
}