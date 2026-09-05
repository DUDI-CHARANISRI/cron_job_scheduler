package com.example.cron_scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpJobEventPublisher implements JobEventPublisher {
    @Override
    public void publish(JobEvent event) {
        // Kafka is disabled by default for local development.
    }
}