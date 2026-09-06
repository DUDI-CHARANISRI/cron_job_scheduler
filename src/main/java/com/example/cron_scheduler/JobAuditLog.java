package com.example.cron_scheduler;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_audit_logs")
public class JobAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobName;
    private String status;
    private LocalDateTime executedAt;
    private Integer attempts;
    private Long durationMs;
    private String errorMessage;

    public JobAuditLog() {}

    public JobAuditLog(String jobName, String status, LocalDateTime executedAt) {
        this(jobName, status, executedAt, 1, 0L, null);
    }

    public JobAuditLog(String jobName, String status, LocalDateTime executedAt,
                       int attempts, long durationMs, String errorMessage) {
        this.jobName = jobName;
        this.status = status;
        this.executedAt = executedAt;
        this.attempts = attempts;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
    public Integer getAttempts() { return attempts; }
    public Long getDurationMs() { return durationMs; }
    public String getErrorMessage() { return errorMessage; }
}