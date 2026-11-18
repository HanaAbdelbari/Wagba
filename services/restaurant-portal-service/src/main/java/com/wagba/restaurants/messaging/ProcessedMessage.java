package com.wagba.restaurants.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** One row per (consumer, event) already handled, so a redelivered event is skipped. */
@Entity
@Table(name = "processed_messages")
@Getter
@NoArgsConstructor
public class ProcessedMessage {

    @EmbeddedId
    private ProcessedMessageId id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedMessage(String consumerName, UUID eventId) {
        this.id = new ProcessedMessageId(consumerName, eventId);
        this.processedAt = Instant.now();
    }
}