# Seat Reservation at Scale — Backend

A Spring Boot REST API for reserving assigned seats for shows. This project is designed around correctness under concurrent booking attempts: a seat must not be confirmed for more than one reservation, multi-seat bookings must be all-or-nothing, per-user limits must remain correct under concurrency, and retries must not create duplicate reservations.
**Live URL** https://seat-reservation-api-lbfw.onrender.com/index.html

## Contents

- [Assignment goals](#assignment-goals)
- [Technology stack](#technology-stack)
- [Architecture](#architecture)
- [Core design and correctness](#core-design-and-correctness)
- [Data model](#data-model)
- [Prerequisites](#prerequisites)
- [Run locally](#run-locally)
- [Configuration](#configuration)
- [Authentication](#authentication)
- [Backend API reference](#backend-api-reference)
- [Errors and HTTP status codes](#errors-and-http-status-codes)
- [Observability](#observability)
- [Load testing](#load-testing)
- [Deployment](#deployment)
- [Operational checks and reconciliation](#operational-checks-and-reconciliation)
- [Known limitations and next steps](#known-limitations-and-next-steps)
- [AI assistance disclosure](#ai-assistance-disclosure)

## Assignment goals

The take-home exercise asks for a JSON REST service that can:

1. Let an administrator create shows and assigned seats.
2. Let an authenticated customer reserve one or more seats.
3. Prevent double-selling when multiple requests target the same seat.
4. Enforce a default maximum of four active seats per customer per show.
5. Support idempotent retries using an `Idempotency-Key`.
6. Commit a multi-seat reservation completely or not at all.
7. Support cancellation and make cancelled seats available again.
8. Report seat availability and reconcile it against persisted state.
9. Expose health/readiness and Prometheus metrics.
10. Provide a repeatable burst/hot-seat load test and explain observed results.

This implementation uses **explicit cancellation** rather than temporary seat holds. Therefore, the held-seat count is currently zero.

## Technology stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot 4.x, Spring MVC |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8.4, InnoDB |
| Schema migration | Flyway |
| Authentication | Spring Security, JWT bearer tokens |
| Validation | Jakarta Bean Validation |
| API testing | Postman / curl |
| Load testing | k6 |
| Packaging / deployment | Docker, Docker Compose, Render |
| Monitoring | Spring Boot Actuator, Micrometer, Prometheus endpoint |

## Architecture

```text
Client / Postman / k6
        |
        | HTTPS + Bearer JWT
        v
Spring Security
        |
        v
REST Controllers
        |
        v
Service layer (@Transactional)
   |       |         |
   |       |         +--> Idempotency record
   |       +------------> User/show quota row
   +--------------------> Seat rows (pessimistic write lock)
        |
        v
Spring Data JPA / Hibernate
        |
        v
MySQL 8.4 (InnoDB)
```

Controllers handle HTTP input/output and authentication context. The service layer owns booking rules and transaction boundaries. MySQL is the durable source of truth.

## Core design and correctness

### Preventing double booking

Reservation is performed inside one database transaction. Requested seat IDs are sorted and the matching seat rows are selected with a pessimistic write lock (`SELECT ... FOR UPDATE` semantics). The service checks that every requested seat belongs to the requested show and is `AVAILABLE` before changing any seat.

The transaction then creates the reservation, creates reservation-seat mappings, changes the seats to `CONFIRMED`, and updates the customer's active-seat quota. Competing requests for the same seat wait for the row lock; after the first transaction commits, the next request sees that the seat is no longer available and receives a conflict response.

The database transaction is essential: if validation or persistence fails partway through, the entire operation rolls back.

### Multi-seat all-or-nothing

The service validates the complete requested seat set before making the booking changes. A request with even one unavailable or invalid seat is rejected; it must not partially reserve the other seats.

### Per-customer quota

The default maximum is four active seats per customer per show. A quota row keyed by `(user_id, show_id)` is created if absent and locked during the reservation transaction. The service checks the new total before confirming seats. Cancellation reduces the active count in the same transaction.

### Idempotency

Clients send an `Idempotency-Key` header. The service associates the key with the authenticated customer, show, and request-body hash.

- Same key + same request: return the stored original result rather than creating another reservation.
- Same key + different request body: return `409 Conflict`.
- A retry must not create another reservation or charge.

Use a new key for a genuinely new booking attempt. The key is scoped to the authenticated user and show in the application design.

### Cancellation

A customer can cancel only their own reservation. Cancellation changes the reservation status to `CANCELLED`, makes its seats `AVAILABLE`, clears the current reservation reference, and reduces the active quota in one transaction. Repeated cancellation should be treated as a conflict or a safe no-op according to the service's current implementation; verify the actual response before documenting it as one or the other.

### Availability

Availability is derived from persisted seat statuses:

- `available`: seats with status `AVAILABLE`
- `confirmed`: seats with status `CONFIRMED`
- `held`: `0` because this version does not implement temporary holds

The application exposes an availability gauge through Actuator/Prometheus. A periodic refresh is useful for a dashboard, but database state remains authoritative.

## Data model

The schema contains these seven application tables:

| Table | Purpose |
|---|---|
| `users` | Customer/admin identity and encoded password |
| `shows` | Show name, integer price in paise, per-user limit |
| `seats` | Seat number, show, status, current reservation reference |
| `reservations` | Booking header, owner, amount, status, timestamps |
| `reservation_seats` | Reservation-to-seat mapping |
| `user_show_quotas` | Active-seat count per user/show |
| `idempotency_records` | Request key, body hash, and stored response |

Primary keys use `BIGINT` auto-increment IDs. Prices are stored as integer paise to avoid floating-point currency calculations. Flyway migration files are under `src/main/resources/db/migration`.

## Prerequisites

- JDK 21
- Maven (or the repository Maven wrapper)
- Docker Desktop / Docker Engine and Docker Compose
- MySQL 8.4 if running the database outside Compose
- k6 for load testing

## Run locally

1. Clone the repository and enter its root directory.
2. Configure the database values in `application.properties` or environment variables.
3. Start MySQL. Example Compose command:

```bash
docker compose up -d mysql
```

4. Start the application:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Or build and run the JAR:

```bash
./mvnw clean package
java -jar target/*.jar
```

Default local application address: `http://localhost:8080`.

Flyway should apply migrations on startup. Do not manually create the same application tables before Flyway runs; doing so can cause migration conflicts. Confirm migration success in startup logs and inspect `flyway_schema_history`.

## Configuration

The following names are used by the application configuration. Keep credentials out of Git and set secure values in the deployment environment.

| Variable / property | Local default or example | Description |
|---|---|---|
| `PORT` | `8080` | HTTP port; hosting platforms may inject this |
| `DB_URL` | `jdbc:mysql://localhost:3307/seat_reservation` | JDBC URL |
| `DB_USERNAME` | `seat_user` | Database user |
| `DB_PASSWORD` | Set locally | Database password |
| `JWT_SECRET` | Development fallback only | JWT signing secret; use a strong random secret in deployment |
| `JWT_EXPIRATION` | `3600000` | Token lifetime in milliseconds |

Example production JDBC URL for a TLS-enabled managed MySQL service:

```text
jdbc:mysql://DB_HOST:DB_PORT/DB_NAME?sslMode=REQUIRED
```

Never commit a real JWT secret, database password, bearer token, or provider credential.

## Authentication

Protected APIs use:

```http
Authorization: Bearer <access-token>
```

Register/login endpoints return an authentication response containing a token, token type, user ID, email, and role. Use the returned token for subsequent requests.

- Customer registration must create a `CUSTOMER`; clients must not be able to self-assign `ADMIN`.
- Admin routes require the `ADMIN` role.
- Reservation ownership is taken from the authenticated principal, not from a client-supplied `userId`.
- If an admin bootstrap/registration endpoint is enabled in a deployment, protect it or disable it after initial setup.

### Example login response shape

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "<access-token>",
    "tokenType": "Bearer",
    "userId": 1,
    "email": "customer@example.com",
    "role": "CUSTOMER"
  }
}
```

The exact message and field values depend on the implementation.

## Backend API reference

All request and response bodies are JSON unless noted. Routes below reflect the implemented API paths discussed for this project. Confirm exact DTO validation limits and response envelopes against the current source before submission.

### Authentication

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register customer |
| `POST` | `/api/auth/login` | Public | Authenticate and receive JWT |
| `POST` | `/api/auth/admin/register` | Deployment-dependent | Admin bootstrap/registration; protect or disable publicly |

#### Register customer

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "name": "Amit",
  "email": "amit@example.com",
  "password": "ChangeThisPassword123!"
}
```

Expected outcome: customer account created and an authentication response returned, subject to the actual `AuthResponse` implementation.

#### Login

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "amit@example.com",
  "password": "ChangeThisPassword123!"
}
```

Use `data.token` as the bearer token for protected routes.

### Shows

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| `GET` | `/api/shows` | Public | List shows |
| `GET` | `/api/shows/{showId}` | Public | Get one show |
| `POST` | `/api/admin/shows` | ADMIN | Create a show |

#### Create show (ADMIN)

```http
POST /api/admin/shows
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "name": "Interstellar",
  "pricePaise": 150000,
  "perUserLimit": 4
}
```

`pricePaise` is an integer amount in paise. For example, `150000` paise is ₹1,500. The per-user limit defaults to four if omitted or configured that way by the service.

### Seats and availability

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/admin/shows/{showId}/seats` | ADMIN | Create seats for a show |
| `GET` | `/api/shows/{showId}/seats` | Public | List seats and their statuses |
| `GET` | `/api/shows/{showId}/availability` | Public | Get available/held/confirmed counts |

#### Create seats (ADMIN)

```http
POST /api/admin/shows/1/seats
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "seatNumbers": ["A1", "A2", "A3", "A4"]
}
```

Use the exact request DTO expected by the running application; if the current DTO accepts one seat number per request rather than a `seatNumbers` array, send the single-seat shape supported by that DTO. Do not assume bulk creation until verified.

#### Example availability response shape

```json
{
  "success": true,
  "message": "Availability fetched successfully",
  "data": {
    "available": 58,
    "held": 0,
    "confirmed": 2
  }
}
```

The envelope/property names may differ in the current DTO; use the actual response as the source of truth.

### Reservations

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/shows/{showId}/reservations` | CUSTOMER | Reserve one or more seats |
| `POST` | `/api/reservations/{reservationId}/cancel` | CUSTOMER | Cancel own reservation |
| `GET` | `/api/reservations/my` | CUSTOMER | List current customer's reservations, if the history controller is included |

#### Reserve seats

```http
POST /api/shows/1/reservations
Authorization: Bearer <customer-token>
Idempotency-Key: booking-unique-001
Content-Type: application/json
```

```json
{
  "seatIds": [10, 11]
}
```

Do not send `userId`; the server derives the customer from the JWT principal. A successful booking returns `201 Created` with a reservation response containing reservation ID, show ID, user ID, seat IDs, amount in paise, status, and creation time.

#### Cancel reservation

```http
POST /api/reservations/123/cancel
Authorization: Bearer <customer-token>
```

The authenticated customer must own reservation `123`. A successful cancellation returns the API's success response and releases the seats transactionally.

#### My reservations

```http
GET /api/reservations/my
Authorization: Bearer <customer-token>
```

Returns the authenticated customer's reservation history if `MyReservationsController` is present and mapped. If the route returns `404`, verify that controller exists under the application component-scan package and that the app was restarted.

### Health and metrics

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| `GET` | `/actuator/health` | Public | Health status |
| `GET` | `/actuator/health/liveness` | Public | Liveness probe, if probes enabled |
| `GET` | `/actuator/health/readiness` | Public | Readiness probe, if probes enabled |
| `GET` | `/actuator/info` | Public | Application info |
| `GET` | `/actuator/metrics` | Public | Metric names |
| `GET` | `/actuator/prometheus` | Public | Prometheus scrape output |

Keep detailed health output restricted in production if it exposes infrastructure details.

## Errors and HTTP status codes

| Status | Meaning | Typical case |
|---|---|---|
| `200 OK` | Request succeeded | Read, login, cancellation response as implemented |
| `201 Created` | Resource/booking created | Successful reservation |
| `400 Bad Request` | Invalid input | Missing fields, empty seat list, invalid IDs |
| `401 Unauthorized` | Authentication required/failed | Missing or invalid JWT, invalid credentials |
| `403 Forbidden` | Authenticated but not allowed | Customer calls admin route |
| `404 Not Found` | Resource/route not found | Unknown show/reservation or missing mapping |
| `409 Conflict` | Business conflict | Seat already booked, quota exceeded, idempotency key reused with different body |
| `500 Internal Server Error` | Unexpected server failure | Unhandled defect or infrastructure error |

For the assignment, contention must be a clean `409`, not a `500`. A conflict response should provide a useful error message and, where implemented, a machine-readable error code/reason.

## Observability

### Health

Use `/actuator/health` to check the app and database health. For deployment readiness, verify that the database health indicator is UP, not just that the HTTP process started.

### Prometheus metrics

The project exposes Actuator's Prometheus endpoint. Custom metric names discussed/used in the implementation include:

- `reservation_confirmed_total`
- `reservation_cancelled_total`
- `reservation_declined_total` (declines should be labeled/categorized by reason where implemented)
- `reservation_available_seats`
- HTTP server request metrics
- Hikari connection-pool metrics

A successful HTTP response alone does not prove the custom business counter incremented. Check the metrics after a known booking and cancellation. Avoid incrementing confirmed metrics for an idempotent replay.

### Logs and correlation IDs

Use structured logs for request/booking lifecycle events and avoid logging passwords, JWTs, or database credentials. If the correlation-ID filter is enabled, include the response `X-Correlation-ID` in support/debugging notes and propagate it through logs.

## Load testing

The repository's k6 script is intended to exercise a hot seat:

```bash
k6 run --summary-export=load-test-results.json seat-reservation-load-test.js
```

Set environment variables locally rather than putting a live token in the script:

PowerShell example:

```powershell
$env:BASE_URL="https://YOUR-DEPLOYMENT-URL"
$env:TOKEN="YOUR_SHORT_LIVED_CUSTOMER_TOKEN"
$env:SHOW_ID="1"
$env:SEAT_ID="1"
k6 run --summary-export=load-test-results.json seat-reservation-load-test.js
```

The current script configuration is a small smoke/burst run (5 VUs for 30 seconds), not a 20,000-concurrent-user benchmark. Use a newly created show with a known available seat and multiple distinct customer tokens for a meaningful hot-seat race. Never paste tokens into README, Git, screenshots, or chat.

### Recorded initial k6 run

The reported run produced:

| Measure | Observed |
|---|---:|
| Iterations / HTTP requests | 38 |
| Booking responses counted as `409` | 38 |
| Booking responses counted as `201` | 0 |
| Check: response was `201` or `409` | 38 passed, 0 failed |
| Mean booking response time | ~4,011.7 ms |
| p95 booking response time | ~6,049.2 ms |
| Maximum booking response time | ~7,486.2 ms |
| Approx. request rate | ~1.13 req/s |

**Interpretation:** This run observed conflict responses only. It does not demonstrate the expected one-winner/many-loser race because no `201` winner was observed; the tested seat may already have been reserved before the run. Repeat against a fresh available seat and verify persisted rows afterward.

The pasted summary also showed a p95 threshold field marked `true` while the reported p95 was about 6.05 seconds against a 5-second threshold. Recheck the raw k6 terminal output and exported JSON before claiming the latency threshold passed. Do not present this small run as proof of 20,000 concurrent-request capacity.

## Deployment

The service has been deployed as a Docker-based Render web service with a managed MySQL database. For a reproducible deployment:

1. Push the Dockerfile, Maven project, and Flyway migrations to the repository.
2. Create the managed MySQL database and verify TLS connection settings.
3. Configure `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `PORT` in the hosting provider.
4. Use a strong newly generated JWT secret; rotate any secret or database credential that was exposed.
5. Deploy and inspect startup logs for successful Flyway migration and Hibernate schema validation.
6. Check `/actuator/health`, then test registration/login and a complete booking/cancellation flow.
7. Confirm `flyway_schema_history` and all expected tables exist in the database.

**Repository:** `ADD_REPOSITORY_URL`  
**Live API:** `ADD_DEPLOYMENT_URL`

## Operational checks and reconciliation

Before submission, run a final reconciliation against the database:

- Every `CONFIRMED` seat has exactly one active confirmed reservation mapping.
- No seat is mapped to multiple active reservations.
- Cancelled reservations no longer hold seats.
- `user_show_quotas.active_seats` matches the count of active seats for each user/show.
- Availability totals equal the total seats created for each show.
- A contested-seat test produces at most one successful reservation.
- Same idempotency key and same body does not create a second reservation.
- Same key with a different body returns `409`.
- Multi-seat failure leaves no partial reservation or seat update.

Useful manual checks (adapt table/column names to the deployed migration):

```sql
SHOW TABLES;
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
SELECT show_id, status, COUNT(*) FROM seats GROUP BY show_id, status;
SELECT user_id, show_id, active_seats FROM user_show_quotas;
```

## Known limitations and next steps

- The current k6 test is a small smoke/burst test, not a validated 20k-concurrency capacity test.
- A single application instance and in-memory custom counters are not sufficient for durable, multi-instance business analytics; use a shared metrics backend and database-derived reconciliation for production.
- Explicit cancellation is implemented instead of expiring holds.
- Add a controlled admin bootstrap process; do not leave admin registration publicly accessible.
- Add multi-token k6 support and separate scenarios for same-seat contention, many-seat throughput, quota races, idempotent retries, and cancellation races.
- Tune connection pool, database capacity, lock wait timeout, request timeout, and load generator resources based on measured results.
- Add integration tests that assert database invariants after concurrent requests.

## AI assistance disclosure

AI tools were used for assistance with planning, documentation structure, troubleshooting, and drafting examples. The implementation, configuration, API behavior, and test results should be reviewed and verified by the author. Any generated text or code was checked against the actual project before submission; unverified behavior is explicitly marked above rather than represented as completed.
