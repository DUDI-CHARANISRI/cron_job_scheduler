/**
 * Core business service for creating, updating, executing, and monitoring cron jobs.
 * This service owns validation, scheduling, retry behaviour, and audit persistence.
 */
package com.example.cron_scheduler;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static java.lang.Math.round;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class JobService {
    private static final String SUCCESS_STATUS = "SUCCESS";
    private static final String FAILED_STATUS = "FAILED";

    private final ScheduledJobRepository jobRepository;
    private final JobAuditRepository auditRepository;
    private final TaskScheduler taskScheduler;
    private final JobEventPublisher eventPublisher;
    private final RestClient restClient = RestClient.create();
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public JobService(ScheduledJobRepository jobRepository, JobAuditRepository auditRepository,
                      TaskScheduler taskScheduler, JobEventPublisher eventPublisher) {
        this.jobRepository = Objects.requireNonNull(jobRepository, "jobRepository must not be null");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository must not be null");
        this.taskScheduler = Objects.requireNonNull(taskScheduler, "taskScheduler must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    /**
     * Returns every configured job in deterministic ID order.
     *
     * @return list of job responses
     */
    @Transactional(readOnly = true)
    public List<JobResponse> findAll() {
        return jobRepository.findAllByOrderByIdAsc().stream()
                .map(JobResponse::from)
                .toList();
    }

    /**
     * Creates a new job and registers it with the scheduler if enabled.
     *
     * @param request job payload to persist
     * @return persisted job representation
     */
    @Transactional
    public JobResponse create(JobRequest request) {
        JobRequest validatedRequest = requireValidRequest(request);
        ensureNameAvailable(validatedRequest.name());
        validateCron(validatedRequest.cronExpression());

        ScheduledJob job = jobRepository.save(new ScheduledJob(
                validatedRequest.name(),
                validatedRequest.cronExpression(),
                normalizeTargetUrl(validatedRequest.targetUrl()),
                validatedRequest.retriesOrDefault()));
        schedule(job);
        return JobResponse.from(job);
    }

    /**
     * Updates an existing job definition and re-registers the schedule.
     *
     * @param id identifier of the job
     * @param request updated job payload
     * @return updated job representation
     */
    @Transactional
    public JobResponse update(Long id, JobRequest request) {
        ScheduledJob job = findJob(id);
        JobRequest validatedRequest = requireValidRequest(request);

        if (!job.getName().equalsIgnoreCase(validatedRequest.name())
                && jobRepository.existsByNameIgnoreCase(validatedRequest.name())) {
            throw new ResponseStatusException(CONFLICT, "A job with this name already exists");
        }

        validateCron(validatedRequest.cronExpression());
        job.update(
                validatedRequest.name(),
                validatedRequest.cronExpression(),
                normalizeTargetUrl(validatedRequest.targetUrl()),
                validatedRequest.retriesOrDefault());
        schedule(jobRepository.save(job));
        return JobResponse.from(job);
    }

    /**
     * Enables or disables a job and re-syncs the scheduler.
     *
     * @param id identifier of the job
     * @param enabled true to enable, false to disable
     * @return updated job representation
     */
    @Transactional
    public JobResponse setEnabled(Long id, boolean enabled) {
        ScheduledJob job = findJob(id);
        job.setEnabled(enabled);
        if (enabled) {
            schedule(job);
        } else {
            cancel(id);
        }
        return JobResponse.from(jobRepository.save(job));
    }

    /**
     * Removes a job and cancels its active schedule.
     *
     * @param id identifier of the job
     */
    @Transactional
    public void delete(Long id) {
        findJob(id);
        cancel(id);
        jobRepository.deleteById(id);
    }

    /**
     * Executes a job immediately outside of its cron schedule.
     *
     * @param id identifier of the job
     * @return audit record for the execution attempt
     */
    @Transactional
    public JobExecutionResponse runNow(Long id) {
        return execute(findJob(id));
    }

    /**
     * Returns the execution history for a specific job, newest first.
     *
     * @param id identifier of the job
     * @return ordered execution history
     */
    @Transactional(readOnly = true)
    public List<JobExecutionResponse> history(Long id) {
        ScheduledJob job = findJob(id);
        return auditRepository.findByJobNameOrderByExecutedAtDesc(job.getName()).stream()
                .map(JobExecutionResponse::from)
                .toList();
    }

    /**
     * Returns aggregated operational metrics for the scheduler dashboard.
     *
     * @return summary metrics including totals, success rate, and latest execution timestamp
     */
    @Transactional(readOnly = true)
    public JobMetricsResponse summary() {
        List<ScheduledJob> jobs = jobRepository.findAllByOrderByIdAsc();
        long totalJobs = jobs.size();
        Map<Boolean, Long> stateCounts = jobs.stream()
                .collect(Collectors.partitioningBy(ScheduledJob::isEnabled, Collectors.counting()));

        long activeJobs = stateCounts.getOrDefault(Boolean.TRUE, 0L);
        long pausedJobs = stateCounts.getOrDefault(Boolean.FALSE, 0L);
        long successfulExecutions = auditRepository.countByStatus(SUCCESS_STATUS);
        long failedExecutions = auditRepository.countByStatus(FAILED_STATUS);
        long totalExecutions = successfulExecutions + failedExecutions;
        double successRate = totalExecutions == 0 ? 0.0 : (successfulExecutions * 100.0) / totalExecutions;

        String lastExecutionAt = auditRepository.findTopByOrderByExecutedAtDesc()
                .map(JobAuditLog::getExecutedAt)
                .map(LocalDateTime::toString)
                .orElse(null);

        return new JobMetricsResponse(
                totalJobs,
                activeJobs,
                pausedJobs,
                totalExecutions,
                successfulExecutions,
                failedExecutions,
                round(successRate * 10.0) / 10.0,
                lastExecutionAt
        );
    }

    /**
     * Re-registers a job schedule after a configuration change.
     *
     * @param job job to reschedule
     */
    public void reschedule(ScheduledJob job) {
        ScheduledJob safeJob = Objects.requireNonNull(job, "job must not be null");
        validateCron(safeJob.getCronExpression());
        schedule(safeJob);
    }

    private void schedule(ScheduledJob job) {
        ScheduledJob safeJob = Objects.requireNonNull(job, "job must not be null");
        cancel(safeJob.getId());
        if (safeJob.isEnabled()) {
            validateCron(safeJob.getCronExpression());
            ScheduledFuture<?> future = taskScheduler.schedule(() -> execute(safeJob),
                    new CronTrigger(safeJob.getCronExpression()));
            scheduledTasks.put(safeJob.getId(), future);
        }
    }

    private JobExecutionResponse execute(ScheduledJob job) {
        ScheduledJob safeJob = Objects.requireNonNull(job, "job must not be null");
        LocalDateTime startedAt = LocalDateTime.now();
        String status = SUCCESS_STATUS;
        String errorMessage = null;
        int attempts = 0;

        for (int attempt = 1; attempt <= safeJob.getMaxRetries() + 1; attempt++) {
            attempts = attempt;
            try {
                String targetUrl = normalizeTargetUrl(safeJob.getTargetUrl());
                if (StringUtils.hasText(targetUrl)) {
                    restClient.post()
                            .uri(targetUrl)
                            .body(Map.of("jobId", safeJob.getId(), "jobName", safeJob.getName()))
                            .retrieve()
                            .toBodilessEntity();
                }
                break;
            } catch (RestClientException exception) {
                status = FAILED_STATUS;
                errorMessage = exception.getMessage();
            }
        }

        long durationMs = java.time.Duration.between(startedAt, LocalDateTime.now()).toMillis();
        JobExecutionResponse execution = JobExecutionResponse.from(auditRepository.save(
                new JobAuditLog(safeJob.getName(), status, startedAt, attempts, durationMs, errorMessage)));
        eventPublisher.publish(JobEvent.from(safeJob, execution));
        return execution;
    }

    private void cancel(Long id) {
        if (id == null) {
            return;
        }
        ScheduledFuture<?> future = scheduledTasks.remove(id);
        if (future != null) {
            future.cancel(false);
        }
    }

    private ScheduledJob findJob(Long id) {
        if (id == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Job id is required");
        }
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Job not found: " + id));
    }

    private void validateCron(String expression) {
        if (!StringUtils.hasText(expression)) {
            throw new ResponseStatusException(BAD_REQUEST, "Cron expression is required");
        }
        try {
            new CronTrigger(expression.trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid cron expression", exception);
        }
    }

    private void ensureNameAvailable(String name) {
        if (!StringUtils.hasText(name) || jobRepository.existsByNameIgnoreCase(name.trim())) {
            throw new ResponseStatusException(CONFLICT, "A job with this name already exists");
        }
    }

    private JobRequest requireValidRequest(JobRequest request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Job payload is required");
        }

        String name = request.name() == null ? null : request.name().trim();
        String cronExpression = request.cronExpression() == null ? null : request.cronExpression().trim();
        String targetUrl = normalizeTargetUrl(request.targetUrl());
        Integer maxRetries = request.maxRetries() == null ? 0 : request.maxRetries();

        if (!StringUtils.hasText(name)) {
            throw new ResponseStatusException(BAD_REQUEST, "Job name is required");
        }
        if (!StringUtils.hasText(cronExpression)) {
            throw new ResponseStatusException(BAD_REQUEST, "Cron expression is required");
        }
        if (maxRetries < 0 || maxRetries > 10) {
            throw new ResponseStatusException(BAD_REQUEST, "Retry count must be between 0 and 10");
        }

        return new JobRequest(name, cronExpression, targetUrl, maxRetries);
    }

    private String normalizeTargetUrl(String targetUrl) {
        if (!StringUtils.hasText(targetUrl)) {
            return null;
        }
        String normalized = targetUrl.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}