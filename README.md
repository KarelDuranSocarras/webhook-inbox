# webhook-inbox

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
![Java 21](https://img.shields.io/badge/Java-21-blue.svg)

A self-hosted, RequestBin-style webhook inspector.
Create an inbox, point any webhook at its ingest URL, and inspect the captured requests — no account, no third-party service.

## Demo

<!-- TODO(karel): record and drop the demo GIF here (docs/demo.gif). -->
_Placeholder — demo GIF coming soon._

## Requirements

- **Docker** (for the PostgreSQL container and for integration tests).
- **Java 21** — only needed if you want to run or build the app from source.

## Quick start (development)

> Running the whole app with `docker compose up` is **coming soon**.
> For now, Docker runs only the database; the app runs from source.

1. Start PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

2. Run the app with the `dev` profile (points at `localhost:5433`):

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. Open <http://localhost:8080> — it redirects to the inbox list.
   Interactive API docs are at <http://localhost:8080/swagger-ui.html>.

## Usage with curl

Create an inbox:

```bash
curl -s -X POST http://localhost:8080/api/inboxes \
  -H 'Content-Type: application/json' \
  -d '{"name":"my first inbox"}'
```

The response contains a `token`. Use it to send a webhook:

```bash
curl -s -X POST http://localhost:8080/in/<token>/orders \
  -H 'Content-Type: application/json' \
  -d '{"event":"order.created","id":42}'
```

List the captured requests for that inbox:

```bash
curl -s "http://localhost:8080/api/inboxes/<token>/requests"
```

Filter by method or by path substring (optional):

```bash
curl -s "http://localhost:8080/api/inboxes/<token>/requests?method=POST&q=orders"
```

The ingest URL is derived from the incoming request by default. To force a
different public base URL (for example behind a proxy), set
`WEBHOOK_INBOX_BASE_URL`.

## Environment variables

| Variable | Default | Description |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5433/webhookinbox` | JDBC URL. **`dev` profile only.** |
| `DB_USER` | `webhookinbox` | Database user. **`dev` profile only.** |
| `DB_PASSWORD` | `webhookinbox` | Database password. **`dev` profile only.** |
| `APP_MAX_BODY_BYTES` | `1048576` | Maximum accepted webhook body size (bytes); larger bodies return `413`. |
| `APP_RETENTION_DAYS` | `7` | Reserved: captured-request retention window. Enforcement lands in a later phase (see Roadmap). |
| `APP_RATE_LIMIT_ENABLED` | `true` | Reserved: enable ingestion rate limiting. Enforcement lands in a later phase (see Roadmap). |
| `APP_RATE_LIMIT_RPM` | `100` | Reserved: ingestion requests per minute per client. Enforcement lands in a later phase (see Roadmap). |

## Screenshots

<!-- TODO(karel): add real screenshots. -->
_Placeholders — screenshots coming soon._

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 (Spring Framework 7) |
| Persistence | Spring Data JPA / Hibernate ORM 7.4, PostgreSQL 16 (JSONB for headers) |
| Migrations | Flyway 12 |
| Web UI | Thymeleaf 3.1 + htmx 2.0.4 (vendored, no CDN) |
| API docs | springdoc-openapi 3.1 (Swagger UI) |
| Testing | JUnit 6, AssertJ, Mockito, Testcontainers 2.0 |
| Build | Maven (wrapper) |

## Architecture

```text
        Browser / webhook sender
                  │
                  ▼
        ┌───────────────────────┐
        │   Spring MVC (web)    │
        │  ├─ UI   (Thymeleaf)  │
        │  ├─ REST (/api/**)    │
        │  └─ Ingest (/in/**)   │
        └───────────┬───────────┘
                    ▼
        ┌───────────────────────┐
        │  Application services │
        └───────────┬───────────┘
                    ▼
        ┌───────────────────────┐
        │ Spring Data JPA       │
        │        │              │
        │        ▼              │
        │   PostgreSQL 16       │
        └───────────────────────┘
                    ▲
             Flyway migrations
```

## Roadmap

- [x] Core domain, persistence, REST API and webhook ingestion
- [x] Web UI (Thymeleaf + htmx)
- [x] Integration tests (API + ingestion, Testcontainers)
- [x] Docs and OpenAPI annotations
- [ ] Ingestion rate limiting (Bucket4j), scheduled retention, validate `inbox.active`
- [ ] Dockerfile, full `docker compose up` (app + DB), CI/CD and GHCR images
- [ ] `v0.1.0` release, public demo and launch

## Contributing

See [`CONTRIBUTING.md`](./CONTRIBUTING.md) _(coming soon)_.

## License

[MIT](./LICENSE)
