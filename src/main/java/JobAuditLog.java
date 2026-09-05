package com.example.cron_scheduler;

import jakarta.persistence.*;
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

    public JobAuditLog() {}

    public JobAuditLog(String jobName, String status, LocalDateTime executedAt) {
        this.jobName = jobName;
        this.status = status;
        this.executedAt = executedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}