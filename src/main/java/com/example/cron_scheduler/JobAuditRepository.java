package com.example.cron_scheduler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobAuditRepository extends JpaRepository<JobAuditLog, Long> {
	java.util.List<JobAuditLog> findByJobNameOrderByExecutedAtDesc(String jobName);
}