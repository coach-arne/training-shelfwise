# ADR-005 — Fines Are Calculated on Demand

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-05-13 |
| Author | Sara Lindqvist |
| Deciders | Steering committee of 2026-05-13 |

## Context

TA-07 v1.0 describes a nightly job (J-01) that adds the daily rate to a running total for every overdue loan. During review, several problems were found:

- The job is not idempotent: if it runs twice in one night, the member is charged twice.
- The grace period cannot be applied correctly: the first day is accrued before it is known whether the copy will be returned within the grace period.
- Corrections to the calendar of closing days, book-drop returns that are backdated, and tariff changes cannot be applied retroactively without complex correction logic.
- The member would see an amount that differs from what the calculation rules in FA-06 produce.

## Decision

Fines are **calculated on demand** from the due date, the end date (return date, current date or lost declaration date), the calendar of closing days and the tariff table. A charge is stored only when the loan is closed or the copy is declared lost. **No running totals are stored.**

## Consequences

- Job J-01 is removed. TA-07 must be updated.
- An endpoint for the accrued fine is needed (TA-05).
- The calculation must be fast enough for lists of overdue loans (target in TA-08).
- The FineCalculator becomes a pure function that can be tested exhaustively.
