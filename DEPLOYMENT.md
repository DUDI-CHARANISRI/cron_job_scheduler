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

```bash
docker build -t cron-job-scheduler .
docker run --rm -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e ADMIN_USERNAME=admin \
  -e ADMIN_PASSWORD_HASH='$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' \
  -e OPERATOR_USERNAME=operator \
  -e OPERATOR_PASSWORD_HASH='$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' \
  cron-job-scheduler
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
