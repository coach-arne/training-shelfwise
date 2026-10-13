# ADR-002 — MySQL 8.0 as Database

| Field | Value |
|---|---|
| Status | Superseded by ADR-004 (2026-03-25) |
| Date | 2026-03-11 |
| Author | Sara Lindqvist |
| Deciders | Sara Lindqvist, Jonas Becker |

## Context

LibTrack 4 runs on MySQL. The delivery team has experience with MySQL, and reusing the same database engine would simplify the data migration.

## Decision

Shelfwise uses MySQL 8.0 as its relational database.

## Consequences

- Data migration from LibTrack 4 can partly reuse existing export scripts.
- The city IT standard for new applications still needs to be verified (action from the kick-off meeting of 2026-03-12).

## Note

This decision was superseded by ADR-004 after city IT confirmed that PostgreSQL is the standard for new applications.
