# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-10-12

First public release: a self-hosted, RequestBin-style webhook inspector.

### Added

- Inbox management REST API (`/api/inboxes`): create, list, fetch and delete.
- Webhook ingestion endpoint (`/in/{token}/**`) capturing method, path, query
  parameters, headers, body and received-at timestamp.
- Web UI (Thymeleaf + htmx, vendored — no CDN): inbox list, inbox detail and
  request detail views.
- Request filtering by HTTP method and path substring.
- Ingestion rate limiting per inbox (Bucket4j), returning `429` with a
  `Retry-After` header when exceeded.
- Scheduled retention job that deletes captured requests older than a
  configurable number of days.
- Active-inbox validation: requests to an inactive inbox are rejected with
  `404`.
- Configurable maximum webhook body size; larger bodies return `413`.
- OpenAPI documentation with Swagger UI (`/swagger-ui.html`).
- Multi-stage, non-root `Dockerfile` and a full `docker compose` stack
  (app + PostgreSQL 16).
- CI (`verify` workflow) and CD (`publish` workflow) publishing multi-arch
  images (`linux/amd64`, `linux/arm64`) to GHCR, with `edge`, `main-<sha8>`,
  `dev-<sha8>`, `vX.Y.Z` and `latest` tags.

### Fixed

- Cast the request filter parameter to string for PostgreSQL `ILIKE` queries.

### Security

- No authentication or authorization yet (UI, REST API and ingestion); planned
  for a later phase.

[Unreleased]: https://github.com/KarelDuranSocarras/webhook-inbox/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/KarelDuranSocarras/webhook-inbox/releases/tag/v0.1.0
