# Security and Enterprise Architecture Notes

## Overview
This project is intentionally structured as a demo-ready enterprise backend for recruiter presentations and learning. It demonstrates secure defaults, layered observability, API access control, and modern integration patterns without depending on paid SaaS services.

## Security model
- Spring Security is enabled with form login, HTTP basic, and optional OAuth2 login support.
- API endpoints are protected and require authenticated access.
- Usernames and BCrypt password hashes are supplied through environment variables; no
	production password is stored in the UI, Java source, or application properties.
- `ADMIN` can create, update, enable, disable, and delete jobs.
- `OPERATOR` can list jobs, inspect execution history, and run jobs manually, but cannot
	change job definitions or delete them.
- H2 console is permitted only for local development and uses same-origin frame protections.
- CSRF is disabled only for the H2 console path to keep local browser access workable.
- TLS/SSL is configurable through environment variables and can be enabled for production-like HTTPS mode.

The checked-in test profile uses a non-production BCrypt test hash and request-level
mock users. Local and production runs must provide their own account values.

## Recommended enterprise extension
- Replace in-memory users with a database-backed identity table.
- Add Spring Authorization Server or Keycloak for real OAuth/OIDC.
- Introduce RBAC as a separate permission matrix for jobs and audit access.
- Enable mTLS or gateway-level TLS in a production deployment.
- Put the app behind an ingress or API gateway with rate limiting and WAF rules.

## Observability and debugging
- Application logs are at INFO/DEBUG levels for the app package.
- Exceptions are centralized via a `@RestControllerAdvice` and captured with stack traces.
- Actuator health endpoints are available for monitoring.
- Flyway migration is enabled to keep schema evolution consistent.

## Database and migration strategy
- Local default uses H2 for simplicity.
- Docker profile uses PostgreSQL.
- Flyway ensures schema is created and versioned.
- This keeps the project learning-friendly while mimicking enterprise database practices.

## Demo guidance
This repository is suitable for showing:
- secure API configuration
- database migrations
- modern Java architecture
- scheduling and orchestration concepts
- GraphQL + REST hybrid API design
- event-driven integration with Kafka
- professional project documentation and a strong GitHub story
