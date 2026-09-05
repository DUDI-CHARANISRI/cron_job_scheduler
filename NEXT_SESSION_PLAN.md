# Cron Scheduler Handoff Plan

## Project

Repository: `cron_job_scheduler`
Branch: `main`
Application: Spring Boot 4.1.1 scheduler service

## What Is Working

The application is a configurable job orchestration service with:

- REST API under `/api/jobs`
- GraphQL endpoint at `/graphql`
- GraphiQL at `/graphiql`
- Persisted scheduled jobs using Spring Data JPA and file-backed H2
- Dynamic cron registration using Spring `TaskScheduler` and `CronTrigger`
- Enable, disable, update, delete, list, and manual run operations
- Job execution history in `JOB_AUDIT_LOGS`
- Optional webhook execution using `targetUrl`
- Retry attempts using `maxRetries`
- Execution status, attempts, duration, and error message
- Optional Kafka event publishing to `job-execution-events`
- Enabled jobs restored after application startup
- H2 console at `/h2-console/`

## Important Files

- `src/main/java/com/example/cron_scheduler/JobService.java`: main scheduling, webhook, retry, audit, and event flow
- `src/main/java/com/example/cron_scheduler/JobController.java`: REST API
- `src/main/java/com/example/cron_scheduler/JobGraphQlController.java`: GraphQL API
- `src/main/resources/graphql/schema.graphqls`: GraphQL schema
- `src/main/java/com/example/cron_scheduler/ScheduledJob.java`: persisted job definition
- `src/main/java/com/example/cron_scheduler/JobAuditLog.java`: execution audit entity
- `src/main/java/com/example/cron_scheduler/KafkaJobEventPublisher.java`: Kafka publisher
- `src/main/resources/application.properties`: H2, GraphQL, and Kafka settings
- `README.md`: project setup and API examples
- `src/test/java/com/example/cron_scheduler/JobControllerIntegrationTest.java`: API integration tests

## Local Run

Start the application:

```bash
./mvnw spring-boot:run
```

API:

```bash
curl http://localhost:8080/api/jobs
```

Create a webhook job:

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"name":"customer-sync","cronExpression":"0 0/5 * * * *","targetUrl":"https://example.com/hooks/customer-sync","maxRetries":2}'
```

Run job 1 manually:

```bash
curl -X POST http://localhost:8080/api/jobs/1/run
```

View history:

```bash
curl http://localhost:8080/api/jobs/1/executions
```

GraphQL query:

```bash
curl http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ jobs { id name cronExpression targetUrl enabled } }"}'
```

H2 console:

```text
http://localhost:8080/h2-console/
```

H2 JDBC URL:

```text
jdbc:h2:file:./data/cron_scheduler
```

H2 user is `sa`; password is empty.

## Kafka

Kafka publishing is disabled by default so the app runs without Kafka:

```properties
app.kafka.enabled=false
```

To enable it, run Kafka on `localhost:9092` and set:

```properties
app.kafka.enabled=true
```

The event topic is `job-execution-events`.

## Validation

Use an isolated in-memory database when a local application is already holding the file-backed H2 lock:

```bash
./mvnw test -q -Dspring.datasource.url=jdbc:h2:mem:validationdb
```

Also useful:

```bash
git diff --check
git status --short
```

## Git Commit Status

Before ending a session, commit the current milestone if it has not already been committed:

```bash
git add .
git commit -m "add graphql kafka and webhook job execution"
git push origin main
```

Do not commit the local H2 files under `data/`; `data/` is already ignored in `.gitignore`.

## Known Limitations

- H2 is used locally; PostgreSQL is not added yet.
- Kafka producer code exists, but Kafka infrastructure and a consumer are not added yet.
- Webhook retry attempts are implemented, but there is no configurable backoff or timeout policy yet.
- The application currently has no authentication or authorization.
- There is no frontend dashboard yet.
- The scheduled execution path uses Spring `TaskScheduler`; Quartz is on the classpath but is not directly used for job registration.
- Jobs without `targetUrl` record simulated successful executions.

## Recommended Next Session

1. Check `git status` and confirm the previous milestone is committed and pushed.
2. Add `docker-compose.yml` with PostgreSQL and Kafka.
3. Add PostgreSQL profile/configuration while retaining H2 for tests.
4. Add webhook timeout and exponential backoff configuration.
5. Add tests for successful webhook calls, failed calls, and retry count.
6. Add a Kafka consumer or notification service to demonstrate event-driven processing.
7. Add Spring Boot Actuator health and metrics endpoints.
8. Improve README with architecture diagram, screenshots, and recruiter-facing project bullets.
9. Add GitHub Actions CI for build and tests.
10. Consider an Angular dashboard only after the backend is stable.

## Resume Talking Point

> Built a Spring Boot job orchestration service with REST and GraphQL APIs, dynamic cron scheduling, persisted execution history, webhook execution with retries, optional Kafka execution events, and restart recovery for enabled jobs.
