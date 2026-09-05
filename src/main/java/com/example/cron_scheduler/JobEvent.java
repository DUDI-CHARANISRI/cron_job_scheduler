package com.example.cron_scheduler;

import java.time.LocalDateTime;

public record JobEvent(Long jobId, String jobName, String status, LocalDateTime executedAt) {
    public static JobEvent from(ScheduledJob job, JobExecutionResponse execution) {
        return new JobEvent(job.getId(), job.getName(), execution.status(), execution.executedAt());
    }
}