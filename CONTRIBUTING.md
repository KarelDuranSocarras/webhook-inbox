# Contributing

Thanks for your interest in `webhook-inbox`. This document covers how to build,
test and propose changes.

## Requirements

- **Java 21**
- **Docker** (PostgreSQL for local development and Testcontainers for
  integration tests)

Use the Maven wrapper (`./mvnw`); no system Maven is assumed.

## Build and test

```bash
# Compile only
./mvnw -DskipTests compile

# Unit tests only (Surefire, *Test)
./mvnw test

# Full verification: unit + integration tests (Failsafe, *IT)
./mvnw verify
```

Integration tests (`*IT`) spin up a PostgreSQL container via Testcontainers, so
Docker must be running. `./mvnw verify` is the primary check and is what CI runs.

Run a single test:

```bash
./mvnw test -Dtest=ClassName
./mvnw verify -Dit.test=ClassName
```

## Running locally

```bash
# Start PostgreSQL (host port 5433)
docker compose up -d postgres

# Run with the dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Then open <http://localhost:8080> (UI) or
<http://localhost:8080/swagger-ui.html> (API docs).

The full container stack (app + database):

```bash
docker compose up --build
```

## Branching and releases

- `development` is the working branch; `main` is stable and always releaseable.
- Open feature PRs against `development`.
- When `development` is stable, open a PR into `main` and merge with a **merge
  commit** (no squash or rebase).
- Release tags `vX.Y.Z` are placed on the `main` merge commit and drive the
  published images (`vX.Y.Z` + `latest`).

See the "Releases and image tags" section of the [README](./README.md) for the
full tag/convention table and [`CHANGELOG.md`](./CHANGELOG.md) for history.

## Conventions

- Base package: `com.karel.webhookinbox`.
- Lombok is available; prefer it for boilerplate.
- Flyway owns the schema (`ddl-auto=validate`); migrations live in
  `src/main/resources/db/migration`.
- Keep unit tests (`*Test`) and integration tests (`*IT`) separate.

## Commit messages

Use short, imperative, conventional-style messages, e.g.:

```text
feat: add request replay endpoint
fix: cast ilike filter parameter to string
docs: document image tag conventions
chore: bump version to 0.1.0
```

## License

By contributing you agree that your contributions are licensed under the
[MIT License](./LICENSE).
