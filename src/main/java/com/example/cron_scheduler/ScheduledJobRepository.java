package com.example.cron_scheduler;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {
	java.util.List<ScheduledJob> findAllByOrderByIdAsc();

	boolean existsByNameIgnoreCase(String name);
}