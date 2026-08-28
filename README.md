# TaskFlow API

A REST backend for team task tracking: workspaces contain projects, projects contain tasks,
tasks carry comments and move through a fixed workflow. Access is scoped per workspace by role,
authentication is stateless (JWT access tokens + rotating refresh tokens).

## Stack

- Java 21, Spring Boot 3.3 (Web, Data JPA, Security, Validation, Actuator)
- PostgreSQL 16, schema managed by Flyway
- JWT via `jjwt`, rate limiting via Bucket4j
- Tests: JUnit 5, Mockito, Testcontainers
- Build: Maven

## Layout

```
domain/        JPA entities and the pieces of behaviour that belong on them
               (task workflow, role comparison, refresh-token validity)
dto/           request records (Bean Validation) and response records
repository/    Spring Data repositories; spec/ holds the task query builder
service/       use cases, transaction boundaries, authorization
web/           REST controllers, thin — they translate HTTP to service calls
security/      JWT parsing/issuing, the auth filter, the principal
config/        security, CORS, rate limiting, OpenAPI, JPA auditing
exception/     domain exceptions + the RFC 7807 handler
```

`WorkspaceAccess` is the single choke point for "can this user do this here" checks. When the
caller isn't a member of a workspace it answers `404`, not `403`, so workspace ids can't be
probed.

## Running locally

Needs a JDK 21 and Docker (for PostgreSQL).

```bash
docker compose up -d db
export TASKFLOW_JWT_SECRET=$(openssl rand -base64 48)
./mvnw spring-boot:run          # or: mvn spring-boot:run
```

The app starts on `http://localhost:8080`. OpenAPI UI is at `/swagger-ui.html`, health at
`/actuator/health`. Flyway creates the schema on first boot.

## Configuration

All settings have working defaults for local use; override via environment variables.

| Variable                 | Default                                  | Notes                                |
|--------------------------|------------------------------------------|--------------------------------------|
| `TASKFLOW_DB_URL`        | `jdbc:postgresql://localhost:5432/taskflow` |                                   |
| `TASKFLOW_DB_USER`       | `taskflow`                               |                                      |
| `TASKFLOW_DB_PASSWORD`   | `taskflow`                               |                                      |
| `TASKFLOW_JWT_SECRET`    | dev-only placeholder                     | set a real 32+ byte secret elsewhere |
| `SERVER_PORT`            | `8080`                                   |                                      |

## API

All paths are under `/api/v1`. Everything except `/auth/**` requires
`Authorization: Bearer <access token>`.

| Method & path                                   | Purpose                                   |
|-------------------------------------------------|-------------------------------------------|
| `POST /auth/register`                           | create an account, returns a token pair   |
| `POST /auth/login`                              | exchange credentials for a token pair     |
| `POST /auth/refresh`                            | rotate the refresh token                  |
| `POST /auth/logout`                             | revoke a refresh token                    |
| `GET  /me`                                      | the current user                          |
| `POST /workspaces`                              | create a workspace (caller becomes OWNER) |
| `GET  /workspaces`                              | workspaces the caller belongs to          |
| `GET  /workspaces/{id}`                         | one workspace                             |
| `GET  /workspaces/{id}/members`                 | list members                              |
| `POST /workspaces/{id}/members`                 | add a member by email (ADMIN+)            |
| `PATCH /workspaces/{id}/members/{userId}`       | change a member's role (ADMIN+)           |
| `DELETE /workspaces/{id}/members/{userId}`      | remove a member (ADMIN+)                  |
| `POST /workspaces/{id}/projects`               | create a project (MEMBER+)                |
| `GET  /workspaces/{id}/projects`               | list projects (`?includeArchived`)        |
| `GET  /projects/{id}`                           | one project                               |
| `PUT  /projects/{id}`                           | update / archive a project (MEMBER+)      |
| `DELETE /projects/{id}`                         | delete a project (ADMIN+)                 |
| `POST /projects/{id}/tasks`                     | file a task (MEMBER+)                     |
| `GET  /projects/{id}/tasks`                     | list tasks, paged & filterable            |
| `GET  /tasks/{id}`                              | one task                                  |
| `PUT  /tasks/{id}`                              | update a task (MEMBER+)                   |
| `PATCH /tasks/{id}/status`                      | move a task along the workflow (MEMBER+)  |
| `DELETE /tasks/{id}`                            | delete a task (MEMBER+)                   |
| `POST /tasks/{id}/comments`                     | comment on a task (MEMBER+)              |
| `GET  /tasks/{id}/comments`                     | list comments                             |
| `DELETE /comments/{id}`                         | delete a comment (author or ADMIN+)      |

Task list filters: `status`, `priority`, `assigneeId`, `search` (title substring), plus
`page`, `size`, `sort`.

Errors are `application/problem+json` (RFC 7807) with a stable `type` URI, e.g.
`https://taskflow.dev/problems/invalid-transition`.

## Domain rules

**Workspace roles** (increasing privilege): `VIEWER < MEMBER < ADMIN < OWNER`. A role check is
"at least X". There is exactly one OWNER per workspace and it can't be reassigned or removed
through the member endpoints.

**Task workflow** — status changes are restricted to these edges:

```
BACKLOG → TODO → IN_PROGRESS → IN_REVIEW → DONE
            ↕         ↕            ↕         ↕
         (back one step is allowed at each stage)
```

Anything else is `422 invalid-transition`.

**Task numbers** are per-project and gap-free (`LV-1`, `LV-2`, …). Creation locks the project
row (`SELECT … FOR UPDATE`) so concurrent creates can't collide on a number — see
`TaskNumberConcurrencyIntegrationTest`.

**Refresh tokens** are opaque random strings; only their SHA-256 hash is stored. Each refresh
rotates the token. Presenting an already-used token revokes every token for that user.

## Tests

```bash
mvn test        # unit + service + web-slice; integration tests skip without Docker
mvn verify      # everything, including Testcontainers integration + concurrency tests
```

Integration tests spin up a throwaway PostgreSQL via Testcontainers and run the real
application context. They are annotated `@Testcontainers(disabledWithoutDocker = true)`, so a
machine without a Docker daemon still gets a green `mvn test`.
