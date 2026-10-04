# Seat Reservation at Scale
## Backend Engineering — Deploy & Observe

**Candidate:** Amitkumar Prajapati  
**Application:** Seat Reservation Service  
**Backend:** Java 21, Spring Boot 4, Spring Security, Spring Data JPA  
**Database:** MySQL 8.4 (InnoDB)  
**Deployment:** Docker on Render  
**Database Hosting:** Aiven MySQL

---

# 1. Overview

The Seat Reservation Service is a RESTful backend application designed to manage show listings, seat availability, customer reservations, and cancellations.

The primary engineering objective is to prevent duplicate seat bookings while maintaining data consistency under concurrent reservation requests.

The application provides:

- Customer authentication using JWT.
- Admin show and seat management.
- Seat reservation and cancellation.
- Database-backed reservation persistence.
- Idempotency support for reservation requests.
- Per-user reservation limits.
- Availability tracking.
- Health checks and Prometheus metrics.
- Docker-based deployment.

# 2. Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4 |
| Security | Spring Security, JWT |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL 8.4 |
| Migration | Flyway |
| Build | Maven |
| Containerization | Docker |
| Deployment | Render |
| Database Hosting | Aiven |
| API Testing | Postman |
| Monitoring | Spring Boot Actuator, Prometheus |

# 3. Architecture

The application follows a layered architecture.

```text
Client / Postman / Frontend
            |
            v
      REST Controllers
            |
            v
       Service Layer
            |
            v
    Spring Data JPA
            |
            v
       MySQL Database
```

### Responsibilities

**Controller Layer**
- Handles HTTP requests and responses.
- Validates incoming request data.
- Exposes authentication, show, seat, and reservation APIs.

**Service Layer**
- Implements business rules.
- Controls reservation transactions.
- Enforces seat availability and user quota constraints.
- Handles reservation and cancellation workflows.

**Repository Layer**
- Performs database operations.
- Uses database locking for critical reservation operations.

**Database Layer**
- Stores users, shows, seats, reservations, reservation-seat mappings, quotas, and idempotency records.

# 4. Database Design

The application uses MySQL with InnoDB transactional tables.

| Table | Purpose |
|---|---|
| users | Customer and admin accounts |
| shows | Show details, pricing, reservation limits |
| seats | Seat inventory and current status |
| reservations | Reservation records |
| reservation_seats | Mapping between reservations and seats |
| user_show_quotas | Per-user, per-show active seat count |
| idempotency_records | Request keys and stored reservation responses |

Primary keys use `BIGINT` auto-increment identifiers.

The database schema is managed through Flyway migration scripts.

# 5. Concurrency and Atomicity

## Problem

Two customers may attempt to reserve the same seat at nearly the same time.

Without concurrency control, both requests could read the seat as available and attempt to confirm it.

## Implemented Approach

The reservation workflow uses a database transaction and pessimistic write locking.

The high-level flow is:

1. Authenticate the customer.
2. Validate the requested show and seat IDs.
3. Normalize and sort the requested seat IDs.
4. Start the reservation transaction.
5. Initialize and lock the customer's show quota row.
6. Check the idempotency key.
7. Lock the requested seat rows.
8. Verify that all requested seats are available.
9. Validate the per-user reservation limit.
10. Create the reservation and seat mappings.
11. Update seat statuses and quota count.
12. Store the idempotency response.
13. Commit the transaction.

If validation fails, the transaction is rolled back.

### Why pessimistic locking?

Pessimistic locking is appropriate for inventory operations where conflicting updates must be serialized.

For a contested seat, concurrent transactions cannot both successfully modify the same locked seat row based on the same available state.

The database transaction ensures the reservation record, seat status, mapping records, quota update, and idempotency record are committed together.

### Multi-seat atomicity

A multi-seat reservation is treated as one transaction.

If any requested seat is unavailable, the complete reservation operation fails rather than confirming only a subset of seats.

# 6. Idempotency

Reservation requests support an idempotency key.

The service stores:

- Customer ID
- Show ID
- Idempotency key
- Request hash
- Reservation ID
- Stored response

The requested seat IDs are sorted before calculating the request hash.

### Expected behavior

| Scenario | Expected result |
|---|---|
| Same key + same request | Return original reservation result |
| Same key + different request | HTTP 409 Conflict |
| New key + available seats | Create a new reservation |
| Retry after successful reservation | No duplicate reservation |

Idempotency records are persisted in MySQL so that request retries can be handled using stored state.

# 7. Reservation Limits

The default per-user reservation limit is four seats per show.

The limit is enforced using the `user_show_quotas` table.

The quota row is locked during reservation processing to prevent concurrent requests from independently passing the same quota check.

The active seat count is updated within the reservation transaction.

# 8. Cancellation

The application supports explicit cancellation.

The cancellation workflow:

1. Loads the reservation.
2. Verifies that the authenticated customer owns it.
3. Locks the reservation row.
4. Validates that it is currently confirmed.
5. Marks the reservation as cancelled.
6. Releases the associated seats.
7. Updates the user's active seat quota.
8. Commits the transaction.

Cancellation is performed transactionally to keep reservation and seat inventory state consistent.

# 9. Availability and Reconciliation

The service exposes show availability information.

The inventory model uses:

- `AVAILABLE`
- `CONFIRMED`

The application does not implement temporary seat holds, so the held count is zero.

Availability is calculated from persisted seat statuses.

The reconciliation objective is to verify that:

- Every confirmed reservation references the expected seats.
- Confirmed seats are not assigned to multiple active reservations.
- Cancelled reservations do not retain confirmed seats.
- User quota counts match active reserved seats.
- Total inventory equals available + held + confirmed.

Database reconciliation queries and actual test output should be included with the final submission.

# 10. Authentication and Authorization

The application uses Spring Security and JWT authentication.

- Customers authenticate before reservation operations.
- Customer identity is obtained from the authenticated security context.
- Admin endpoints require the ADMIN role.
- Public customer registration creates CUSTOMER accounts.
- Passwords are stored using password hashing.

The API does not rely on a client-supplied user ID to establish reservation ownership.

# 11. Deployment

The application is containerized using Docker.

### Deployment components

```text
GitHub Repository
       |
       v
Render Docker Build
       |
       v
Spring Boot Container
       |
       v
Aiven MySQL
```

Configuration is supplied through environment variables.

Important configuration includes:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `PORT`

Secrets are not intended to be committed to the source repository.

Flyway is used to manage database migrations, while Hibernate schema validation checks the entity-to-database mapping.

### Live URLs

- **Repository:** [Add GitHub URL]
- **Application:** [Add Render URL]
- **Health:** [Add `/actuator/health` URL]
- **Prometheus:** [Add `/actuator/prometheus` URL]

# 12. Observability

The application exposes Spring Boot Actuator endpoints.

### Health

`/actuator/health`

Used to check application and database health.

### Prometheus

`/actuator/prometheus`

Metrics include:

- HTTP request counts and timings.
- Available seat gauge.
- Reservation confirmation counter.
- Reservation cancellation counter.
- Reservation decline counter.

Structured application logs are used to trace important reservation and cancellation events.

### Monitoring objective

During load testing, observe:

- Successful reservation count.
- Conflict response count.
- HTTP 5xx errors.
- Request latency.
- Database connection pool utilization.
- Remaining available seats.
- Final database consistency.

# 13. Testing Strategy

The following test scenarios are relevant to the assignment.

| Test | Expected outcome | Actual result |
|---|---|---|
| Customer registration | Successful registration | [Fill] |
| Customer login | JWT returned | [Fill] |
| Admin creates show | Show created | [Fill] |
| Admin creates seats | Seats created | [Fill] |
| Reserve available seats | HTTP 201 | [Fill] |
| Reserve same seat again | HTTP 409 | [Fill] |
| Same idempotency key and body | Original response | [Fill] |
| Same key with different body | HTTP 409 | [Fill] |
| Cancel reservation | Reservation cancelled | [Fill] |
| Availability after cancellation | Released seats available | [Fill] |
| Concurrent same-seat requests | One winner | [Fill] |
| Final reconciliation | No inconsistent inventory | [Fill] |

## Load Test Results

**Tool:** [Add tool/script name]  
**Concurrent requests:** [Actual number]  
**Duration:** [Actual duration]  
**Successful reservations:** [Actual count]  
**Conflict responses:** [Actual count]  
**HTTP 5xx responses:** [Actual count]  
**Latency:** [Actual measurements]  

No performance result should be reported unless it was measured.

# 14. Engineering Trade-offs

### Pessimistic locking

Provides straightforward correctness for conflicting seat reservations, but hot seats can cause waiting and lock contention.

### Database connection pool

The application uses a bounded connection pool. A high number of incoming requests does not imply the same number of simultaneous database connections.

### Explicit cancellation

The implementation uses explicit cancellation instead of temporary holds, reducing the complexity of expiration and cleanup jobs.

### Single database

MySQL provides transactional consistency for the current implementation. At larger scale, database contention and connection limits would need further evaluation.

# 15. Future Improvements

- Add automated concurrent load testing with configurable virtual users.
- Measure p50, p95, and p99 latency.
- Add durable metrics collection and dashboards.
- Add distributed tracing.
- Add rate limiting and request throttling.
- Improve handling of database lock timeouts.
- Add automated reconciliation alerts.
- Evaluate queue-based admission control for extreme traffic bursts.
- Add integration tests using a disposable MySQL instance.
- Add CI/CD checks for clean deployment and migration validation.

# 16. AI Usage Disclosure

AI tools were used as development assistance for understanding requirements, exploring implementation approaches, troubleshooting errors, and improving documentation.

The implementation, configuration, deployment, API testing, and final verification were carried out and reviewed by the candidate.

All reported test results should reflect actual execution.

---

**End of Write-up**