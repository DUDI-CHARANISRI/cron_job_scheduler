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
    public int retriesOrDefault() { return maxRetries == null ? 0 : maxRetries; }
}