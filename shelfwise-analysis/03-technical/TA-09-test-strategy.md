# TA-09 — Test Strategy

| Field | Value |
|---|---|
| Document ID | TA-09 |
| Version | 1.0 |
| Status | Approved |
| Owner | Jonas Becker, Backend Lead |
| Last updated | 2026-05-12 |
| Related | TA-02, TA-08, FA-06 |

## 1. Test levels

| Level | Scope | Tools |
|---|---|---|
| Unit (backend) | Domain logic, in particular the FineCalculator and due date calculation | JUnit 5, AssertJ |
| Unit (frontend) | Components and services | Jest |
| Integration | Application services with the database | Spring Boot tests, Testcontainers (PostgreSQL 16) |
| Architecture | Module boundaries as described in TA-01 §4 | ArchUnit |
| End-to-end | Key user journeys in both Angular applications | Playwright |
| Performance | Targets of TA-08 | Gatling |
| User acceptance | Business scenarios by key users | Manual, in the acceptance environment |

## 2. Coverage targets

- Backend overall: 80% line coverage.
- `fines` module: 95% line coverage and a mutation score of at least 80% (PIT).
- Coverage targets are checked in the pipeline; a drop below target fails the build.

## 3. Test data and time

- Tests use synthetic data only. No production data outside production; migration rehearsals use masked data.
- Tests that depend on the current date use a fixed clock.
- The closing days of 2026 from `06-reference/public-holidays-2026.csv` are loaded as test fixture.

## 4. Golden test cases for fine calculation

These test cases are taken from the worked examples in FA-06 §3.8. They form the acceptance baseline for the FineCalculator. Changing an expected value requires approval of the product owner.

| ID | Member | Material | Replacement cost | Due date | Returned | Expected fine |
|---|---|---|---|---|---|---|
| TC-FIN-01 | ADULT | BOOK | €25.00 | 2026-03-02 | 2026-03-03 | €0.00 |
| TC-FIN-02 | ADULT | BOOK | €25.00 | 2026-03-02 | 2026-03-04 | €0.50 |
| TC-FIN-03 | ADULT | BOOK | €25.00 | 2026-03-06 | 2026-03-13 | €1.75 |
| TC-FIN-04 | ADULT | BOOK | €25.00 | 2026-04-04 | 2026-04-08 | €0.50 |
| TC-FIN-05 | SENIOR | BOOK | €25.00 | 2026-03-02 | 2026-03-07 | €0.63 |
| TC-FIN-06 | ADULT | AUDIOVISUAL | €8.00 | 2026-03-02 | 2026-03-16 | €8.00 |

## 5. User acceptance testing

- Period: December 2026, in the acceptance environment.
- Testers: Ellen Ward and one librarian per branch.
- Exit criteria: no open defects of severity 1 or 2; all fine scenarios accepted by Finance.

## 6. Defect management

Defects are registered as GitLab issues with severity 1 (blocking) to 4 (cosmetic). Severity 1 and 2 defects are reviewed daily during UAT.
