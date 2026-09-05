package com.example.cron_scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ScheduledTaskService {

    private final JobAuditRepository auditRepository;

    public ScheduledTaskService(JobAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    // Runs every 30 seconds
    @Scheduled(cron = "0/30 * * * * ?")
    public void executeSampleJob() {
        JobAuditLog log = new JobAuditLog("SampleCronJob", "SUCCESS", LocalDateTime.now());
        auditRepository.save(log);
        System.out.println("Cron Job Executed & Logged at: " + LocalDateTime.now());
    }
}