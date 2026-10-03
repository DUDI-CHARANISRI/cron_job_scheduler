package com.example.cron_scheduler;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Actuator endpoint exposing scheduler state for admin debugging.
 */
@Endpoint(id = "scheduler")
@Component
public class SchedulerEndpoint {
    private final JobService jobService;

    public SchedulerEndpoint(JobService jobService) {
        this.jobService = jobService;
    }

    @ReadOperation
    public Map<String, Object> status() {
        return Map.of(
                "scheduledJobIds", jobService.getScheduledJobIds(),
                "scheduledCount", jobService.getScheduledJobIds().size(),
                "lastRehydratedIds", jobService.getLastRehydratedIds(),
                "lastRehydratedAt", jobService.getLastRehydratedAt()
        );
    }
}
