/**
 * Aggregate operational metrics used by the scheduler dashboard.
 */
package com.example.cron_scheduler;

public record JobMetricsResponse(
        long totalJobs,
        long activeJobs,
        long pausedJobs,
        long totalExecutions,
        long successfulExecutions,
        long failedExecutions,
        double successRate,
        String lastExecutionAt
) {
}
