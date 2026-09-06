package com.example.cron_scheduler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobAuditRepository extends JpaRepository<JobAuditLog, Long> {
    List<JobAuditLog> findByJobNameOrderByExecutedAtDesc(String jobName);
}