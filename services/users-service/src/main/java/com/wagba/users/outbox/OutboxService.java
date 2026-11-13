package com.wagba.users.outbox;

import com.wagba.users.common.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository repository;

    /** MANDATORY: the event is saved in the same transaction as the business change, or not at all. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String aggregateType, UUID aggregateId, int aggregateVersion,
                        String eventType, Map<String, Object> payload) {
        OutboxMessage m = new OutboxMessage();
        m.setId(UUID.randomUUID());
        m.setAggregateType(aggregateType);
        m.setAggregateId(aggregateId);
        m.setAggregateVersion(aggregateVersion);
        m.setEventType(eventType);
        m.setCorrelationId(currentCorrelationId());
        m.setPayload(payload);
        m.setOccurredAt(Instant.now());
        repository.save(m);
    }

    private static UUID currentCorrelationId() {
        String id = MDC.get(CorrelationIdFilter.MDC_KEY);
        return id == null ? null : UUID.fromString(id);
    }
}