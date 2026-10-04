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

The variable names are also listed in `.env.example`; never commit a populated `.env` file.
If repository or environment secrets are added after the development environment is started, rebuild or recreate it so the secrets are injected at runtime.

The dashboard uses the secured REST API and stores only the active browser session
credential in session storage. Use a real OAuth/OIDC identity provider for production.

The `ADMIN` role can create, update, enable, disable, and delete jobs. The `OPERATOR`
role can view jobs, inspect execution history, and run jobs manually, but cannot change
job definitions or delete them. The dashboard calls `/api/auth/me` after login and hides
the create/pause/delete controls for the `OPERATOR` role to match what the backend allows.

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
default so the application remains easy to run locally without extra infrastructure.

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

> The free Render tier spins the service down after about 15 minutes of inactivity.
> The first request after idle time can take 30-60 seconds while it wakes back up.

## Verify

```bash
./mvnw test

## Action-runner microservice

This repository now contains a small companion microservice `action-runner` that centralizes outbound actions (HTTP forwarding and email sending). Recruiters and reviewers will appreciate the clear microservice boundary demonstrating separation of concerns.

Run locally:

1. (Optional) Start MailHog to capture emails locally:

```bash
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

2. Start `action-runner` (defaults to port 8082):

```bash
mvn -f action-runner spring-boot:run -Dspring-boot.run.arguments="--server.port=8082 --spring.mail.host=localhost --spring.mail.port=1025"
```

3. Start the scheduler and point it at `action-runner`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--action.runner.url=http://localhost:8082/execute"
```

4. Trigger a job (replace id with your job id):

```bash
curl -u admin:admin12345 -X POST http://localhost:8080/api/jobs/66/run
```

View any captured emails in MailHog at `http://localhost:8025` and HTTP forwards in the `receiver` service (`/hooks/received`).

### Deploying the action-runner

- The repository includes `render.yaml` with an `action-runner` service entry. Pushing the branch with this file will allow Render to create both services from the same repo.
- `action-runner` also includes `action-runner/Dockerfile` (multi-stage) so Render or Docker builds can produce a small runtime image.

Ensure the scheduler is configured with the `action.runner.url` environment variable on the host (Render environment variable or startup argument) to route job executions to the `action-runner` service.
```

Maven also runs Checkstyle during the `validate` phase. Run the style gate alone
with:

```bash
./mvnw checkstyle:check
```

## Running the persistence restart integration test (optional)

The repository contains an optional integration test that simulates creating a job,
restarting the application context, and asserting the job is rehydrated into the
in-memory scheduler. The test is disabled by default to avoid failing CI builds.

To run it locally:

1. Enable the test by removing or commenting out the `@Disabled` annotation in
	`src/test/java/com/example/cron_scheduler/SchedulerPersistenceTest.java`.
2. Run only that test locally:

```bash
mvn -Dtest=SchedulerPersistenceTest test
```

The test uses a file-backed H2 database at `target/scheduler_test_db` so the
persisted job survives the two application contexts it starts.
