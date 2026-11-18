package com.wagba.restaurants.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProcessedMessageId implements Serializable {

    @Column(name = "consumer_name", nullable = false, length = 150)
    private String consumerName;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;
}