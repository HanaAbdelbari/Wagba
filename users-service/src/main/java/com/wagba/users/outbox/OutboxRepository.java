package com.wagba.users.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {

    // SKIP LOCKED: if two instances poll at once, each gets different rows
    @Query(value = """
            SELECT * FROM outbox_messages
            WHERE published_at IS NULL
              AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
            ORDER BY occurred_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessage> lockBatch(@Param("now") Instant now, @Param("limit") int limit);
}