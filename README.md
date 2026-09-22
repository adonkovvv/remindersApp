# remindersApp

REST API for managing reminders — the backend for a web or mobile reminders client.

Spring Boot 3.5 / Java 21 / PostgreSQL 16. JWT auth, recurring reminders, and a background
dispatcher that delivers notifications when reminders come due.

## Running it

Start Postgres:

```bash
docker compose up -d db
```

Then the app:

```bash
mvn spring-boot:run
```

It listens on `http://localhost:8080`. Flyway applies the schema on first boot.

### Configuration

Everything has a working local default, so nothing is required to run against the compose
database. Override through environment variables for anything real:

| Variable | Default | Notes |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/remindersApp` | Database name is case sensitive |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `JWT_SECRET` | a dev placeholder | **Must** be replaced outside local dev; at least 32 bytes |
| `CORS_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Comma separated |

Notification tuning lives under `app.notifications.*` in `application.properties`:
scan interval, batch size, worker count, queue capacity, retry ceiling, claim timeout.

Interactive docs are at `http://localhost:8080/swagger-ui.html` once the app is up.

## API

All `/api/reminders/**` routes need `Authorization: Bearer <token>`.

### Auth

```
POST /api/auth/register    {email, password, displayName?}  -> {token, tokenType, expiresIn}
POST /api/auth/login       {email, password}                -> {token, tokenType, expiresIn}
GET  /api/auth/me                                           -> {id, email, displayName, createdAt}
```

`/me` needs the token; the other two are open.

### Reminders

```
GET    /api/reminders                 ?completed=&dueBefore=&dueAfter=&page=&size=&sort=
POST   /api/reminders                 {title, description?, dueAt, recurrence?}
GET    /api/reminders/{id}
PUT    /api/reminders/{id}            {title?, description?, dueAt?, completed?, recurrence?}
PATCH  /api/reminders/{id}/complete
POST   /api/reminders/{id}/snooze     {by?}    // ISO-8601 duration, e.g. "PT30M"
DELETE /api/reminders/{id}
```

`recurrence` is one of `NONE`, `DAILY`, `WEEKLY`, `MONTHLY`.

Errors come back as RFC 7807 problem documents. A reminder owned by a different user is
reported as `404`, not `403`, so the API doesn't leak which ids exist.

## How notification dispatch works

A scheduled scan runs every 30s. Each round has three phases, kept deliberately separate:

1. **Claim** — one short transaction grabs a bounded batch with
   `select ... for update skip locked`. `skip locked` is the key: a worker walks past rows
   another worker already holds instead of queueing behind them, so throughput scales with
   worker count rather than serialising on the oldest reminders. Claimed rows are stamped
   with the instance id.
2. **Send** — the batch fans out across `notificationExecutor`, a bounded pool, holding no
   database locks. The queue is bounded and overflow uses `CallerRunsPolicy`, which pushes
   work back onto the scan thread and throttles the loop instead of growing an unbounded
   in-memory backlog.
3. **Record** — each outcome commits in its own transaction, so one failure can't roll back
   the rest of the batch.

Sending inside the claiming transaction would hold row locks for the length of a network
call. Splitting the phases is what keeps this safe to run as more than one instance.

A reminder claimed by a process that then died is picked up again once its claim goes stale
(`app.notifications.claim-timeout`). Delivery is therefore at-least-once, not exactly-once —
senders should tolerate a repeat.

Concurrent edits from the API are guarded separately, by an optimistic `@Version` column;
a losing write surfaces as `409` rather than silently overwriting.

### Senders

`NotificationSender` is pluggable and selected by `app.notifications.sender`:

- `log` (default) — writes to the application log, useful in dev
- `email` — sends through `JavaMailSender`, needs the usual `spring.mail.*` settings

## Tests

```bash
mvn test
```

Integration tests run against a real PostgreSQL container via Testcontainers, so Docker
needs to be available. Postgres-specific behaviour — `skip locked`, partial indexes, the
`lower(email)` unique index — is the whole point of those tests, and H2 would quietly do
the wrong thing for all three.
