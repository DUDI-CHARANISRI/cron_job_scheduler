package com.example.cron_scheduler;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableScheduling
public class CronSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CronSchedulerApplication.class, args);
	}

	@Bean
	CommandLineRunner schedulePersistedJobs(ScheduledJobRepository repository, JobService jobService) {
		return args -> {
			java.util.List<ScheduledJob> enabled = repository.findAll().stream()
				.filter(ScheduledJob::isEnabled)
				.toList();
			enabled.forEach(job -> jobService.reschedule(job));
			jobService.recordRehydratedIds(enabled.stream().map(ScheduledJob::getId).toList());
		};
	}

}
