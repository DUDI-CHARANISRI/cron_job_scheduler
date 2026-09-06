package com.example.cron_scheduler;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class JobService {
    private final ScheduledJobRepository jobRepository;
    private final JobAuditRepository auditRepository;
    private final TaskScheduler taskScheduler;
    private final JobEventPublisher eventPublisher;
    private final RestClient restClient = RestClient.create();
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public JobService(ScheduledJobRepository jobRepository, JobAuditRepository auditRepository,
                      TaskScheduler taskScheduler, JobEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.auditRepository = auditRepository;
        this.taskScheduler = taskScheduler;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<JobResponse> findAll() {
        return jobRepository.findAllByOrderByIdAsc().stream()
                .map(JobResponse::from).toList();
    }

    @Transactional
    public JobResponse create(JobRequest request) {
        ensureNameAvailable(request.name());
        validateCron(request.cronExpression());
        ScheduledJob job = jobRepository.save(new ScheduledJob(request.name(), request.cronExpression(),
            request.targetUrl(), request.retriesOrDefault()));
        schedule(job);
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse update(Long id, JobRequest request) {
        ScheduledJob job = findJob(id);
        if (!job.getName().equalsIgnoreCase(request.name()) && jobRepository.existsByNameIgnoreCase(request.name())) {
            throw new ResponseStatusException(CONFLICT, "A job with this name already exists");
        }
        validateCron(request.cronExpression());
        job.update(request.name(), request.cronExpression(), request.targetUrl(), request.retriesOrDefault());
        schedule(jobRepository.save(job));
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse setEnabled(Long id, boolean enabled) {
        ScheduledJob job = findJob(id);
        job.setEnabled(enabled);
        if (enabled) schedule(job); else cancel(id);
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public void delete(Long id) {
        findJob(id);
        cancel(id);
        jobRepository.deleteById(id);
    }

    @Transactional
    public JobExecutionResponse runNow(Long id) { return execute(findJob(id)); }

    @Transactional(readOnly = true)
    public List<JobExecutionResponse> history(Long id) {
        ScheduledJob job = findJob(id);
        return auditRepository.findByJobNameOrderByExecutedAtDesc(job.getName()).stream()
                .map(JobExecutionResponse::from).toList();
    }

    public void reschedule(ScheduledJob job) {
        validateCron(job.getCronExpression());
        schedule(job);
    }

    private void schedule(ScheduledJob job) {
        cancel(job.getId());
        if (job.isEnabled()) {
            ScheduledFuture<?> future = taskScheduler.schedule(() -> execute(job),
                    new CronTrigger(job.getCronExpression()));
            scheduledTasks.put(job.getId(), future);
        }
    }

    private JobExecutionResponse execute(ScheduledJob job) {
        LocalDateTime startedAt = LocalDateTime.now();
        String status = "SUCCESS";
        String errorMessage = null;
        int attempts = 0;

        for (int attempt = 1; attempt <= job.getMaxRetries() + 1; attempt++) {
            attempts = attempt;
            try {
                if (job.getTargetUrl() != null && !job.getTargetUrl().isBlank()) {
                    restClient.post().uri(job.getTargetUrl())
                            .body(Map.of("jobId", job.getId(), "jobName", job.getName()))
                            .retrieve().toBodilessEntity();
                }
                break;
            } catch (RestClientException exception) {
                status = "FAILED";
                errorMessage = exception.getMessage();
            }
        }

        long durationMs = java.time.Duration.between(startedAt, LocalDateTime.now()).toMillis();
        JobExecutionResponse execution = JobExecutionResponse.from(auditRepository.save(
                new JobAuditLog(job.getName(), status, startedAt, attempts, durationMs, errorMessage)));
        eventPublisher.publish(JobEvent.from(job, execution));
        return execution;
    }

    private void cancel(Long id) {
        ScheduledFuture<?> future = scheduledTasks.remove(id);
        if (future != null) future.cancel(false);
    }

    private ScheduledJob findJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Job not found: " + id));
    }

    private void validateCron(String expression) {
        try {
            new CronTrigger(expression);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid cron expression", exception);
        }
    }

    private void ensureNameAvailable(String name) {
        if (jobRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(CONFLICT, "A job with this name already exists");
        }
    }
}