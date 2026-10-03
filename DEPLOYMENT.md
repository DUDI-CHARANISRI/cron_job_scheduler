# Deployment Guide

This project is ready for a simple, portfolio-friendly deployment flow using Docker and Render.

## 1. Prerequisites

- GitHub account
- Render account
- Docker installed locally (optional, for local validation)
- GitHub repository connected to Render

## 2. Required environment variables

Set these in Render or in your local shell before running the app:

```bash
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD_HASH='$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
export OPERATOR_USERNAME=operator
export OPERATOR_PASSWORD_HASH='$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
```

> Use a real bcrypt hash in production. Do not commit a populated `.env` file.

## 3. Local Docker validation

### Option A — Docker Compose (recommended for local dev)

1. Create a `.env` from `.env.example` and fill values:

```bash
cp .env.example .env
# edit .env and set a secure POSTGRES_PASSWORD
```

2. Start Postgres and Kafka for local validation:

```bash
docker compose up -d
```

3. Build and run the app (or run from your IDE):

```bash
./mvnw -DskipTests package
java -jar target/cron-scheduler-0.0.1-SNAPSHOT.jar
```

The app will pick up Postgres from the environment variables in `.env` (see `docker-compose.yml`).

### Option B — Single container run

For quick checks without Docker Compose, run a local Postgres (or a free hosted DB) and then:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/cron_scheduler
export SPRING_DATASOURCE_USERNAME=cron_user
export SPRING_DATASOURCE_PASSWORD=changeme
./mvnw -DskipTests spring-boot:run
```

## 4. Deploy to Render

1. Create a new Web Service in Render.
2. Connect your GitHub repository.
3. Choose the repository root.
4. Render will use the provided `render.yaml` and `Dockerfile`.
5. Add environment variables in Render:
   - `ADMIN_USERNAME`
   - `ADMIN_PASSWORD_HASH`
   - `OPERATOR_USERNAME`
   - `OPERATOR_PASSWORD_HASH`
   - `SERVER_PORT=8080`
6. Deploy.

## 5. CI/CD flow

GitHub Actions is already configured in `.github/workflows/ci.yml` to run Maven tests and package the app on pushes and PRs.

The workflow in `.github/workflows/deploy-render.yml` triggers a Render deploy hook when changes land on `main`.

## 6. Notes

- This project is made for portfolio/demo use and uses H2 by default.
- For a production-grade deployment, move to managed Postgres and add TLS, OAuth/OIDC, and monitoring.
- Keep environment secrets outside the repo.

## 7. Free hosted Postgres options (quick setup)

These services offer free tiers suitable for portfolio demos. After creating a DB, set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` in Render or in your repository/deployment secrets.

- ElephantSQL: very simple, provides a Postgres URL. Use the provided `DATABASE_URL` or construct `jdbc:postgresql://` URL.
- Supabase: full Postgres instance with a dashboard and connection strings. Good for demos and has generous free tier.
- Railway: often offers free credit-based projects; check current limits.

Example Render env var when using a managed Postgres instance:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/cron_scheduler
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
```

## 8. Dev tips

- Add secrets through your CI/CD or hosting provider's environment settings, or store them as repository secrets when using automated builds.
- For a stable local dev experience, prefer running the app against the `docker-compose.yml` Postgres service or a remote managed Postgres (Supabase/ElephantSQL). This avoids differences caused by temporary local environments.

## 9. What to highlight in interviews

- The app supports both embedded H2 for quick demos and PostgreSQL for production — explain how `SPRING_DATASOURCE_URL` toggles behavior.
- Show the startup rehydration flow: `CronSchedulerApplication.schedulePersistedJobs` → `JobService.reschedule()` → `JobService.schedule()` registers tasks in `TaskScheduler`.
- Demonstrate persistence by creating a job, restarting the app, and showing the job still present and scheduled.
- Mention monitoring/observability ideas: expose a `/actuator/health` and custom endpoint that lists registered scheduled job IDs (safe for admin only).
