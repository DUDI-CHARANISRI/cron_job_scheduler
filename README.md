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

Open `http://localhost:8080/` for the operations dashboard. The application does not
contain default usernames or passwords. Configure the local accounts before starting:

```bash
export ADMIN_USERNAME=your-admin-name
export ADMIN_PASSWORD_HASH='your-bcrypt-hash'
export OPERATOR_USERNAME=your-operator-name
export OPERATOR_PASSWORD_HASH='your-bcrypt-hash'
./mvnw spring-boot:run
```

The variable names are also listed in `.env.example`; never commit a populated `.env`
file. If Codespaces Secrets were added after this Codespace was created, rebuild or
recreate the Codespace so they are injected into the environment.

The dashboard uses the secured REST API and stores only the active browser session
credential in session storage. Use a real OAuth/OIDC identity provider for production.

The `ADMIN` role can create, update, enable, disable, and delete jobs. The `OPERATOR`
role can view jobs, inspect execution history, and run jobs manually, but cannot change
job definitions or delete them.

## Docker / real-world stack

Start PostgreSQL and Kafka locally:

```bash
docker compose up -d postgres zookeeper kafka
export POSTGRES_USER=your-postgres-user
export POSTGRES_PASSWORD='your-postgres-password'
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

The `docker` profile uses PostgreSQL and Kafka settings from `application-docker.properties`.

Health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

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

Job listings are ordered by the database through a Spring Data derived query rather
than sorting the full result set in application memory. Flyway migration `V2` adds a
composite index for the audit-history lookup and descending execution-time sort.

The event publisher is also the boundary for a future microservices deployment. See
[MICROSERVICES_ARCHITECTURE.md](MICROSERVICES_ARCHITECTURE.md) for the planned scheduler,
execution-worker, and audit/notification service split.

## Deployment

This project is ready for a lightweight public deployment path using Docker and Render.

### Quick deploy flow

```bash
# build locally
./mvnw test
./mvnw package -DskipTests
docker build -t cron-job-scheduler .
```

Then connect the repository to Render or trigger the deploy hook from GitHub Actions.
See [DEPLOYMENT.md](DEPLOYMENT.md) for the step-by-step setup.

## Verify

```bash
./mvnw test
```

Maven also runs Checkstyle during the `validate` phase. Run the style gate alone
with:

```bash
./mvnw checkstyle:check
```
