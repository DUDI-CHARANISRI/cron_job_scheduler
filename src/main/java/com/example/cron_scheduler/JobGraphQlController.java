package com.example.cron_scheduler;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class JobGraphQlController {
    private final JobService jobService;

    public JobGraphQlController(JobService jobService) {
        this.jobService = jobService;
    }

    @QueryMapping
    public List<JobResponse> jobs() {
        return jobService.findAll();
    }

    @QueryMapping
    public List<JobExecutionResponse> executions(@Argument Long jobId) {
        return jobService.history(jobId);
    }

    @MutationMapping
    public JobResponse createJob(@Argument String name, @Argument String cronExpression,
                                 @Argument String targetUrl,
                                 @Argument Integer maxRetries) {
        return jobService.create(new JobRequest(name, cronExpression, targetUrl, maxRetries));
    }

    @MutationMapping
    public JobExecutionResponse runJob(@Argument Long jobId) {
        return jobService.runNow(jobId);
    }
}