/**
 * Request payload used when creating or updating a scheduled task.
 */
package com.example.cron_scheduler;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record JobRequest(
        @NotBlank String name,
        @NotBlank String cronExpression,
        String targetUrl,
        @Min(0) @Max(10) Integer maxRetries
) {
    /**
     * Returns the configured retry count or zero when no explicit value was supplied.
     *
     * @return normalized retry count
     */
    public int retriesOrDefault() {
        return maxRetries == null ? 0 : maxRetries;
    }
}