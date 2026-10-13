# ADR-004 — PostgreSQL 16 as Database

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-03-25 |
| Author | Sara Lindqvist |
| Deciders | Sara Lindqvist, Jonas Becker, city IT (platform team) |
| Supersedes | ADR-002 |

## Context

At the kick-off meeting it was noted that city IT is standardising on PostgreSQL. City IT has confirmed that new applications must use the managed PostgreSQL service; MySQL is no longer offered for new applications.

## Decision

Shelfwise uses **PostgreSQL 16**, provided as a managed service by city IT.

## Consequences

- ADR-002 is superseded.
- The data model (TA-04), the environments (TA-10) and the architecture overview (TA-01) must be updated.
- PostgreSQL features such as partial indexes, NUMERIC types and jsonb can be used.
- The LibTrack 4 migration requires a conversion step from MySQL to PostgreSQL; this was needed anyway because the data model changes.
