/**
 * JPA entity representing a single scheduled job definition.
 */
package com.example.cron_scheduler;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "scheduled_jobs")
public class ScheduledJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String cronExpression;
    private String targetUrl;
    private boolean enabled;
    private int maxRetries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ScheduledJob() {}

    public ScheduledJob(String name, String cronExpression, String targetUrl, int maxRetries) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.cronExpression = Objects.requireNonNull(cronExpression, "cronExpression must not be null");
        this.targetUrl = targetUrl;
        this.maxRetries = maxRetries;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, String cronExpression, String targetUrl, int maxRetries) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.cronExpression = Objects.requireNonNull(cronExpression, "cronExpression must not be null");
        this.targetUrl = targetUrl;
        this.maxRetries = maxRetries;
        this.updatedAt = LocalDateTime.now();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCronExpression() { return cronExpression; }
    public String getTargetUrl() { return targetUrl; }
    public boolean isEnabled() { return enabled; }
    public int getMaxRetries() { return maxRetries; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}