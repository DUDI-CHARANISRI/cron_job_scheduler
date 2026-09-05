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
		return args -> repository.findAll().stream()
				.filter(ScheduledJob::isEnabled)
				.forEach(job -> jobService.reschedule(job));
	}

}
