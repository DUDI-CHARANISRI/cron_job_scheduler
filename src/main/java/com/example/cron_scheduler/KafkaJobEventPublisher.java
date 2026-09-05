package com.example.cron_scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaJobEventPublisher implements JobEventPublisher {
    private final KafkaTemplate<String, JobEvent> kafkaTemplate;

    public KafkaJobEventPublisher(KafkaTemplate<String, JobEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(JobEvent event) {
        kafkaTemplate.send("job-execution-events", String.valueOf(event.jobId()), event);
    }
}