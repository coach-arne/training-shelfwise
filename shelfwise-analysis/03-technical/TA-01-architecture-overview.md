# TA-01 — Architecture Overview

| Field | Value |
|---|---|
| Document ID | TA-01 |
| Version | 1.3 |
| Status | Approved |
| Owner | Sara Lindqvist, Lead Architect |
| Last updated | 2026-05-18 |
| Related | ADR-001 to ADR-005, TA-02 to TA-10 |

## 1. Architectural style

Shelfwise is a **modular monolith** (ADR-001): one Spring Boot backend, deployed as a single unit, internally divided into modules with explicit boundaries. Two Angular single-page applications use the backend's REST API.

## 2. Context

```
   +---------------------+          +----------------------+
   |   Member portal     |          |    Staff console     |
   |   (Angular SPA)     |          |    (Angular SPA)     |
   +----------+----------+          +-----------+----------+
              |        HTTPS, JSON, /api/v1       |
              +----------------+-----------------+
                               |
                 +-------------v--------------+        +------------------+
                 |   Shelfwise backend        |------->|  PostgreSQL 16   |
                 |   (Spring Boot, Java 21)   |        +------------------+
                 +---+-----------+--------+---+
```

## 3. Technology stack

| Layer | Choice | Reference |
|---|---|---|
| Backend language | Java 21 (LTS) | |
| Backend framework | Spring Boot 3.4: Spring Web, Spring Data JPA (Hibernate), Spring Security (OAuth2 resource server) | TA-02 |
| Database | PostgreSQL 16, managed service of city IT | ADR-004 |
| Schema migrations | Flyway | TA-04 |
| Build | Maven | |
| Frontend | Angular 19, standalone components, signals for state, Angular Material; no NgRx | ADR-003, TA-03 |
| Frontend applications | Two applications in one Angular workspace: `member-portal` and `staff-console` | ADR-003 |
| Identity and access | Keycloak 25 (OpenID Connect) | TA-06 |
| Online payments | PayBridge (hosted checkout and webhooks) | TA-07 |
| Messaging | Notify gateway (city service) for e-mail and SMS | TA-07 |
| Hosting | OpenShift on the city private cloud | TA-10 |
| CI/CD | GitLab CI | TA-10 |

## 4. Backend modules

| Module | Responsibility | May depend on |
|---|---|---|
| membership | Members, categories, membership validity, blocking status | shared |
| catalogue | Titles, copies, copy statuses | shared |
| lending | Checkout, return, renewal, due dates | membership, catalogue, shared |
| reservations | Reservation queue, holds | membership, catalogue, lending, shared |
| fines | Fine calculation, charges, waivers | membership, catalogue, lending, shared |
| payments | Desk and online payments, allocation | fines, membership, shared |
| notifications | Composing and sending notifications | all modules (read only), shared |
| reporting | Reports R-01 to R-07 | all modules (read only), shared |
| shared | Calendar of closing days, audit, common types | — |

Module boundaries are verified by architecture tests (TA-09). Modules communicate through public application services, never through each other's repositories.

## 5. Key architecture decisions

| ADR | Decision | Status |
|---|---|---|
| ADR-001 | Modular monolith | Accepted |
| ADR-002 | MySQL 8.0 as database | Superseded by ADR-004 |
| ADR-003 | Angular 19 with standalone components and signals; two applications | Accepted |
| ADR-004 | PostgreSQL 16 as database | Accepted |
| ADR-005 | Fines are calculated on demand; no running totals are stored | Accepted |

## 6. Cross-cutting concerns

- **Time and calendar:** business dates (due date, return date) are local dates in the library's time zone (Europe/Brussels). Technical timestamps are stored in UTC. All date logic uses an injectable clock so it can be tested.
- **Fine calculation:** on demand, see TA-02 §5 and ADR-005.
- **Audit:** all financial operations and changes to configuration are recorded in an audit log (TA-06 §6).
- **Observability:** structured logging with correlation IDs, metrics exported to Prometheus (TA-10).
