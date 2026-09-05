package com.example.cron_scheduler;

import java.time.LocalDateTime;

public record JobExecutionResponse(Long id, String jobName, String status, LocalDateTime executedAt,
                                   Integer attempts, Long durationMs, String errorMessage) {
    public static JobExecutionResponse from(JobAuditLog log) {
        return new JobExecutionResponse(log.getId(), log.getJobName(), log.getStatus(), log.getExecutedAt(),
            log.getAttempts(), log.getDurationMs(), log.getErrorMessage());
    }
}