/**
 * Public representation of a scheduled job returned by the application APIs.
 */
package com.example.cron_scheduler;

import java.time.LocalDateTime;

public record JobResponse(Long id, String name, String cronExpression, String targetUrl, boolean enabled,
                          int maxRetries, LocalDateTime createdAt, LocalDateTime updatedAt) {
    /**
     * Converts the persistence entity to the API contract DTO.
     *
     * @param job domain entity to convert
     * @return response DTO for API consumers
     */
    public static JobResponse from(ScheduledJob job) {
        return new JobResponse(job.getId(), job.getName(), job.getCronExpression(), job.getTargetUrl(), job.isEnabled(),
                job.getMaxRetries(), job.getCreatedAt(), job.getUpdatedAt());
    }
}