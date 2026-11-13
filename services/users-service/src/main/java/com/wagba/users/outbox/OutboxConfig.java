package com.wagba.users.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class OutboxConfig {

    @Bean
    public TopicExchange eventsExchange(@Value("${wagba.events.exchange}") String name) {
        return new TopicExchange(name, true, false); // durable, not auto-delete
    }
}