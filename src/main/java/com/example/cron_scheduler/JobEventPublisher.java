package com.example.cron_scheduler;

public interface JobEventPublisher {
    void publish(JobEvent event);
}