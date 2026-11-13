package com.wagba.users.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRoutingKeyTest {

    @Test
    void camelCaseEventTypes_becomeDottedLowercaseRoutingKeys() {
        assertThat(OutboxPublisher.routingKey("UserRegistered")).isEqualTo("user.registered");
        assertThat(OutboxPublisher.routingKey("UserStatusChanged")).isEqualTo("user.status.changed");
        assertThat(OutboxPublisher.routingKey("PasswordResetRequested")).isEqualTo("password.reset.requested");
    }
}