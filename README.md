# Reliable Job Orchestrator

A Spring Boot service for creating and operating cron-based jobs through a REST API.
It persists job definitions and execution history, supports enable/disable and manual
execution, and restores enabled schedules when the application restarts.

## Tech stack

- Java 17, Spring Boot 4, Spring Web MVC
- Spring Data JPA and H2 for local development
- Spring scheduling with Quartz on the classpath
- Maven and JUnit

## Run locally

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080/api/jobs`.

GraphQL is available at `http://localhost:8080/graphql` and GraphiQL is available
at `http://localhost:8080/graphiql`.

## API examples

Create a job. Spring cron expressions use six fields, including seconds.

```bash
curl -X POST http://localhost:8080/api/jobs \
	-H 'Content-Type: application/json' \
	-d '{"name":"refresh-customer-cache","cronExpression":"0/30 * * * * *","targetUrl":"https://example.com/hooks/cache","maxRetries":2}'
```

```bash
curl http://localhost:8080/api/jobs
curl -X POST http://localhost:8080/api/jobs/1/run
curl http://localhost:8080/api/jobs/1/executions
curl -X PATCH http://localhost:8080/api/jobs/1/disable
curl -X PATCH http://localhost:8080/api/jobs/1/enable
```

When `targetUrl` is provided, each execution sends a `POST` request with this JSON
payload:

```json
{"jobId":1,"jobName":"refresh-customer-cache"}
```

The service retries failed webhook calls up to `maxRetries` additional times and
stores `status`, `attempts`, `durationMs`, and `errorMessage` in the execution log.
Jobs without a target URL remain useful as scheduler demonstrations and record a
successful simulated execution.

## GraphQL example

```bash
curl http://localhost:8080/graphql \
	-H 'Content-Type: application/json' \
	-d '{"query":"{ jobs { id name cronExpression enabled } }"}'
```

Execution events can be published to Kafka topic `job-execution-events` by setting
`app.kafka.enabled=true` and running Kafka on `localhost:9092`. Kafka is disabled by
default so the application remains easy to run in a Codespace without infrastructure.

## Design notes

`ScheduledJob` is the source of truth for job configuration. `JobService` owns
validation, schedule registration, cancellation, manual execution, and audit writes.
The startup runner re-registers enabled jobs loaded from the database, so a restart
does not silently lose the schedule.

## Verify

```bash
./mvnw test
```
