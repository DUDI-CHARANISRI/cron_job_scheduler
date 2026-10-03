package com.example.cron_scheduler;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;

 

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Integration test; enable locally to run persistence restart scenario")
class SchedulerPersistenceTest {

    @Test
    void jobPersistsAcrossRestartAndIsRehydrated() {
        String fileUrl = "jdbc:h2:file:target/scheduler_test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
        // Activate a test profile that points to a file-backed H2 DB (see
        // src/test/resources/application-persistence.properties)
        System.setProperty("spring.profiles.active", "persistence");

        SpringApplication app = new SpringApplication(CronSchedulerApplication.class);

        // Start first context and create a job
        ConfigurableApplicationContext ctx1 = app.run();
        try {
            ScheduledJobRepository repo = ctx1.getBean(ScheduledJobRepository.class);
            JobService jobService = ctx1.getBean(JobService.class);

            ScheduledJob job = new ScheduledJob("persistence-test-job", "0 0 0 1 1 ?", null, 0);
            job = repo.save(job);
            // ensure scheduler registers it in this context
            jobService.reschedule(job);
            Long id = job.getId();
            assertThat(id).isNotNull();
            assertThat(jobService.getScheduledJobIds()).contains(id);
        } finally {
            ctx1.close();
        }

        // Start a fresh context to simulate restart
        ConfigurableApplicationContext ctx2 = app.run();
        try {
            JobService jobService2 = ctx2.getBean(JobService.class);
            // the startup runner should have rehydrated enabled jobs and recorded their ids
            assertThat(jobService2.getLastRehydratedIds()).isNotEmpty();
            // and the scheduled ids should include our job id persisted earlier
            assertThat(jobService2.getLastRehydratedIds()).contains(repoIdFromFile());
        } finally {
            ctx2.close();
        }
    }

    // Helper: read the persisted id from the H2 file-based database metadata using JDBC
    private Long repoIdFromFile() {
        String url = "jdbc:h2:file:target/scheduler_test_db;MODE=PostgreSQL";
        String query = "SELECT id FROM scheduled_jobs ORDER BY id ASC LIMIT 1";
        try (var conn = java.sql.DriverManager.getConnection(url, "sa", "")) {
            try (var stmt = conn.createStatement(); var rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
