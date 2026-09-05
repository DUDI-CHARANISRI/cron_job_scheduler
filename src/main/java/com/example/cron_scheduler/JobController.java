package com.example.cron_scheduler;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) { this.jobService = jobService; }

    @GetMapping
    public List<JobResponse> findAll() { return jobService.findAll(); }

    @PostMapping
    public ResponseEntity<JobResponse> create(@Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(201).body(jobService.create(request));
    }

    @PutMapping("/{id}")
    public JobResponse update(@PathVariable Long id, @Valid @RequestBody JobRequest request) {
        return jobService.update(id, request);
    }

    @PatchMapping("/{id}/enable")
    public JobResponse enable(@PathVariable Long id) { return jobService.setEnabled(id, true); }

    @PatchMapping("/{id}/disable")
    public JobResponse disable(@PathVariable Long id) { return jobService.setEnabled(id, false); }

    @PostMapping("/{id}/run")
    public JobExecutionResponse runNow(@PathVariable Long id) { return jobService.runNow(id); }

    @GetMapping("/{id}/executions")
    public List<JobExecutionResponse> history(@PathVariable Long id) { return jobService.history(id); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return ResponseEntity.noContent().build();
    }
}