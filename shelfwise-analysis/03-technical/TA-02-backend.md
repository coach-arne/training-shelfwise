# TA-02 — Backend Design (Java / Spring Boot)

| Field | Value |
|---|---|
| Document ID | TA-02 |
| Version | 1.2 |
| Status | Approved |
| Owner | Jonas Becker, Backend Lead |
| Last updated | 2026-05-19 |
| Related | TA-01, TA-04, TA-05, TA-07, ADR-005, FA-04, FA-06 |

## 1. Scope

This document describes the internal design of the Shelfwise backend: code organisation, layering, handling of money and dates, the fine calculation, validation, concurrency and error handling.

## 2. Code organisation

- One Maven project, one deployable Spring Boot application.
- One top-level package per module (see TA-01 §4), for example `membership`, `lending`, `fines`.
- Each module exposes a small set of public application services. Domain classes and repositories are package-private.

## 3. Layering

| Layer | Responsibility |
|---|---|
| API | REST controllers, request and response models, input validation, mapping to application services |
| Application | Use-case orchestration, transactions, authorisation checks |
| Domain | Business rules (for example the fine calculator), entities, value objects |
| Persistence | Spring Data JPA repositories, Flyway migrations |

Controllers never call repositories directly.

## 4. Money, dates and time

- All monetary amounts are represented as `BigDecimal` with scale 2 and stored as `NUMERIC(10,2)` in PostgreSQL. Floating-point types must never be used for money.
- Business dates (checkout date, due date, return date) are `LocalDate` values in the time zone Europe/Brussels.
- Technical timestamps are `Instant` values stored in UTC.
- A `Clock` is injected wherever the current date is needed, so that tests can fix the date.

## 5. Fine calculation

### 5.1 On-demand calculation

In line with ADR-005, fines are calculated on demand. For an open loan, the accrued fine is calculated when it is requested. When a loan is closed, or the copy is declared lost, the final fine is calculated once and stored as a charge. No running totals are stored.

### 5.2 Inputs

The `FineCalculator` in the `fines` module is a pure domain service. Inputs:

- due date;
- end date: the return date, the current date (accrued fine) or the lost declaration date;
- member category;
- material type;
- replacement cost of the copy;
- the calendar of closing days (provided by the `CalendarService` in `shared`);
- the tariff table (database table `fine_tariff`, maintained by administrators).

### 5.3 Algorithm

1. Count the chargeable days: opening days from the day after the due date up to and including the end date.
2. If the number of chargeable days is 0 or 1, the fine is zero (grace period).
3. Multiply the chargeable days by the daily rate for the member category and material type.
4. For SENIOR members, multiply the result by 0.50.
5. Round to two decimals using `RoundingMode.HALF_EVEN`.
6. Apply the maximum of €10.00 per copy.

### 5.4 Lost declaration

The lost declaration job (TA-07, J-03) calls the `FineCalculator` with the declaration date as end date and creates three charges: the overdue fine, the replacement cost and the administration fee.

### 5.5 Testability

The calculator has no dependencies on the database or the system clock. Golden test cases are defined in TA-09 §4.

## 6. Business validation

Checkout, renewal, return and reservation rules are implemented in the application services of the `lending` and `reservations` modules, exactly as described in FA-04 and FA-05. They are not repeated here, to avoid two sources of truth.

## 7. Concurrency

- Optimistic locking (a `version` column) on `copy`, `loan`, `reservation` and `charge`.
- A copy can only be checked out once: the check on copy status and the creation of the loan happen in one transaction; a version conflict results in HTTP 409.
- Assignment of returned copies to the reservation queue is serialised per title.

## 8. Error handling

Errors are returned as problem details (RFC 9457) with a machine-readable error code:

| Error code | HTTP | Situation |
|---|---|---|
| MEMBER_BLOCKED | 409 | Member is blocked (balance or expired membership) |
| LOAN_LIMIT_REACHED | 409 | Maximum number of simultaneous loans reached |
| COPY_NOT_AVAILABLE | 409 | Copy is not AVAILABLE and not on hold for this member |
| RENEWAL_NOT_ALLOWED | 409 | Renewal refused; a reason code is included |
| RESERVATION_LIMIT_REACHED | 409 | Maximum number of active reservations reached |
| WAIVER_LIMIT_EXCEEDED | 403 | A librarian tries to waive more than the limit for the role |
| VALIDATION_FAILED | 400 | Invalid input |
| CONCURRENT_MODIFICATION | 409 | Optimistic locking conflict |

## 9. Logging and audit

- Structured JSON logs with a correlation ID per request.
- No personal data in log messages other than internal IDs.
- Financial operations (charges, waivers, payments, refunds) and configuration changes are written to the audit log (see TA-06 §6).

## 10. Configuration

- Spring profiles per environment (dev, tst, acc, prd).
- Secrets (database credentials, PayBridge keys) are provided as OpenShift secrets, never in the repository.
