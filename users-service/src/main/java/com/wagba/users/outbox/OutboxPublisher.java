package com.wagba.users.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    @Value("${wagba.events.exchange}")
    private String exchange;

    @Value("${wagba.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${wagba.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxMessage> batch = repository.lockBatch(Instant.now(), batchSize);

        for (OutboxMessage m : batch) {
            try {
                send(m);
                m.setPublishedAt(Instant.now());
            } catch (Exception e) {
                int attempts = m.getAttemptCount() + 1;
                m.setAttemptCount(attempts);
                long delaySeconds = Math.min(300L, 1L << Math.min(attempts, 8)); // 2s, 4s, ... max 5 min
                m.setNextAttemptAt(Instant.now().plusSeconds(delaySeconds));
                log.warn("Outbox: failed to publish {} ({}), attempt {}, retry in {}s: {}",
                        m.getEventType(), m.getId(), attempts, delaySeconds, e.toString());
                break; // broker is probably down: stop this round, try again later
            }
        }
    }

    private void send(OutboxMessage m) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", m.getId().toString());
        envelope.put("eventType", m.getEventType());
        envelope.put("version", m.getEventVersion());
        envelope.put("occurredAt", m.getOccurredAt().toString());
        envelope.put("correlationId", m.getCorrelationId() == null ? null : m.getCorrelationId().toString());
        envelope.put("aggregateType", m.getAggregateType());
        envelope.put("aggregateId", m.getAggregateId().toString());
        envelope.put("aggregateVersion", m.getAggregateVersion());
        envelope.put("payload", m.getPayload());

        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setMessageId(m.getId().toString());
        props.setType(m.getEventType());
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        Message message = new Message(jsonMapper.writeValueAsBytes(envelope), props);

        String routingKey = routingKey(m.getEventType());

        // wait for the broker's confirmation before marking the row as published
        rabbitTemplate.invoke(t -> {
            t.send(exchange, routingKey, message);
            t.waitForConfirmsOrDie(5_000);
            return true;
        });
    }

    /** UserStatusChanged -> user.status.changed to make them unified*/
    static String routingKey(String eventType) {
        return eventType.replaceAll("([a-z0-9])([A-Z])", "$1.$2").toLowerCase();
    }
}