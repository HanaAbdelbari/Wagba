package com.wagba.users.verification;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationChallengeRepository extends JpaRepository<VerificationChallenge, UUID> {

    // row lock: parallel guesses are serialized, so the attempts limit can't be bypassed
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VerificationChallenge> findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
            UUID userId, ChallengePurpose purpose);

    Optional<VerificationChallenge> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(
            UUID userId, ChallengePurpose purpose);

    @Modifying(flushAutomatically = true)
    @Query("update VerificationChallenge c set c.consumedAt = :now "
            + "where c.userId = :userId and c.purpose = :purpose and c.consumedAt is null")
    int invalidateActive(@Param("userId") UUID userId,
                         @Param("purpose") ChallengePurpose purpose,
                         @Param("now") Instant now);
}