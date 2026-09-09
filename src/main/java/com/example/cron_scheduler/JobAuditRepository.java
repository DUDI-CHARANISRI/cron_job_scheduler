/**
 * Persistence layer for scheduler execution logs and summary metrics.
 */
package com.example.cron_scheduler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobAuditRepository extends JpaRepository<JobAuditLog, Long> {
    List<JobAuditLog> findByJobNameOrderByExecutedAtDesc(String jobName);
    Optional<JobAuditLog> findTopByOrderByExecutedAtDesc();
    long countByStatus(String status);
}