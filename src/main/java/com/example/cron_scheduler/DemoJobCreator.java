package com.example.cron_scheduler;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Optional demo job creator. When `DEMO_CREATE_JOB=true` and `DEMO_TARGET_URL` is set
 * the application will create a demo job on startup if one with the same name does not
 * already exist. This avoids needing admin credentials to create a demo job on a live
 * instance used for recruiter demos.
 */
@Component
public class DemoJobCreator implements CommandLineRunner {

    private final JobService jobService;

    public DemoJobCreator(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void run(String... args) throws Exception {
        String enabled = System.getenv("DEMO_CREATE_JOB");
        String target = System.getenv("DEMO_TARGET_URL");
        if (!"true".equalsIgnoreCase(enabled) || !StringUtils.hasText(target)) {
            return;
        }

        String name = System.getenv().getOrDefault("DEMO_JOB_NAME", "demo-webhook");
        String cron = System.getenv().getOrDefault("DEMO_CRON", "0/15 * * * * *");

        // avoid duplicate demo job creation by checking existing jobs
        boolean exists = jobService.findAll().stream()
                .anyMatch(j -> name.equalsIgnoreCase(j.name()));
        if (exists) {
            return;
        }

        JobRequest req = new JobRequest(name, cron, target, 0);
        jobService.create(req);
    }
}
