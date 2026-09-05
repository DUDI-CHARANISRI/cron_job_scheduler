package com.example.cron_scheduler;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
public class CronSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CronSchedulerApplication.class, args);
	}

}
