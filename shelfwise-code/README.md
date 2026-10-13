# Shelfwise

A lending system for the public libraries of Northwick (increments 1 and 2 of the functional specification).

| Part | Technology |
|---|---|
| Database | PostgreSQL 16 (Flyway migrations, Hibernate `ddl-auto=validate`) |
| Back-end | Java 21, Spring Boot 3.3, Spring Security (JWT), Hibernate/JPA |
| Front-end | Angular 20, Angular Material, signals |

## Run everything in Docker

```bash
docker compose up --build
```

- Front-end: <http://localhost:4200> (nginx; it proxies `/api` to the back-end)
- Back-end API: <http://localhost:8080> (`/actuator/health`)
- PostgreSQL: `localhost:5432` (database, user and password `shelfwise`)

The database is seeded with the situation of **Monday 2026-03-16** (Appendix B). Training mode is on by default
(`SHELFWISE_TRAINING_MODE` in `compose.yaml`): the login page lists the seed accounts, and administrators get the
training screen (business date, daily processing, reset, outbox) and the simulated payment provider.
Set it to `false` for a production-like run; the business date is then the real date.

Accounts (password `shelfwise`): `admin`, `manager.cen`, `librarian.cen`, `librarian.riv`; members log in with a card
number (`NPL000001` to `NPL000019`, scenarios S-01 to S-19).

## Tests

Back-end unit tests and integration tests (against a real PostgreSQL through Testcontainers):

```bash
cd backend && mvn test
```

No JDK or Docker-in-Docker? Run the tests in a container against a throw-away PostgreSQL:

```bash
docker network create shelfwise-test
docker run -d --rm --name shelfwise-test-db --network shelfwise-test -e POSTGRES_DB=shelfwise \
  -e POSTGRES_USER=shelfwise -e POSTGRES_PASSWORD=shelfwise postgres:16-alpine
docker run --rm --network shelfwise-test -e TEST_DB_URL=jdbc:postgresql://shelfwise-test-db:5432/shelfwise \
  -v "$PWD/backend:/app" -v shelfwise-m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn -B test
```

Front-end unit tests: `cd frontend && npx ng test --watch=false` (headless Chrome, set `CHROME_BIN` if needed).

### Traceability (Appendix A)

Every acceptance example is a test whose `@DisplayName` starts with its id (`AC-...`), so `grep -r "AC-FIN-15" backend/src/test`
finds it. Pure calculations (A.1 due dates, A.3 fines, A.7 hold periods) are in `unit/`, the rest are integration tests.

| Examples | Test class |
|---|---|
| AC-LEN-01 to 10, 12, 13 | `unit/CalendarAndDueDateTest`, `LendingIT` |
| AC-MEM-01 to 10 | `MemberIT`, `unit/MemberRulesTest` |
| AC-FIN-01 to 13 | `unit/FineCalculatorTest`, `FinanceIT` |
| AC-FIN-14 to 23 | `FinanceIT` |
| AC-PAY-01 to 05 | `FinanceIT` |
| AC-RES-01 to 10 | `ReservationIT`, `unit/CalendarAndDueDateTest` |
| AC-NOT-01 to 05 | `NotificationIT` |
| AC-ROL-01, 02, 03, 05 | `ApiIT` |

Not covered because the feature belongs to increment 3: AC-LEN-11 (book drop) and AC-ROL-04 (report R-03).
Choices that the specification leaves open are in [`ASSUMPTIONS.md`](ASSUMPTIONS.md).

## Layout

```
compose.yaml            db + backend + frontend
backend/                Spring Boot application (be.npl.shelfwise.<feature>.{entity,repository,service,helper,web})
frontend/               Angular app: /catalogue, /portal, /staff/**, /pay/:reference
```
