# Shelfwise — Analysis Documentation

**Lending system for Northwick Public Library (NPL)**

| Field | Value |
|---|---|
| Document set version | 1.3 |
| Last updated | 2026-05-20 |
| Maintained by | Tom Hollis, Business Analyst |
| Status | Analysis phase — approved for build |

## Purpose

This repository contains the business, functional and technical analysis for **Shelfwise**, the system that will replace LibTrack 4 as the lending (circulation) system of Northwick Public Library. Shelfwise covers members, titles and copies, loans and renewals, reservations, fines and payments, notifications and operational reporting.

## Folder structure

| Folder | Contents |
|---|---|
| `01-business/` | Project charter and glossary |
| `02-functional/` | Functional analysis (FA-01 to FA-09): what the system must do |
| `03-technical/` | Technical analysis (TA-01 to TA-10): how the system will be built (Java / Spring Boot backend, Angular frontend) |
| `04-decisions/` | Architecture Decision Records (ADR-001 to ADR-005) |
| `05-meetings/` | Minutes of decision-making meetings |
| `06-reference/` | Business rules catalogue and reference data |
| `CHANGELOG.md` | Version history of all documents |

## Document conventions

- Every document starts with a header table: document ID, version, status, owner and last updated date.
- Status values: Draft, In review, Approved, Superseded.
- Business rules are identified as `BR-<AREA>-<nn>` and consolidated in `06-reference/BR-catalogue.md`.
- Use cases: `UC-<AREA>-<nn>`. Notifications: `N-<nn>`. Reports: `R-<nn>`. Batch jobs: `J-<nn>`.
- Amounts are in euro (EUR) with two decimals. Dates follow ISO 8601 (YYYY-MM-DD).
- "Member" is the official term for a registered library user. Do not use "patron" or "customer".

## Precedence rules

When documents contradict each other, apply these rules in order:

1. A decision recorded in an **accepted ADR** or in **approved meeting minutes** takes precedence over any analysis document that predates that decision.
2. Otherwise, the **most recent approved version** prevails.
3. The functional analysis (FA) defines **what** the system does; the technical analysis (TA) defines **how**. A TA document may not change functional behaviour. If it does, the FA prevails and the deviation must be raised with the product owner.
4. If these rules do not resolve the conflict, escalate to the product owner (Hannah Mercer). Do not pick a value yourself.

## Open issues

| ID | Description | Owner | Status |
|---|---|---|---|
| OI-01 | PayBridge contract signature pending | Raj Malhotra | Open |
| OI-02 | Timing of e-book lending integration (phase 2) | Hannah Mercer | Open |
| OI-03 | LibTrack 4 historic loans older than 12 months: migrate in anonymised form or archive outside Shelfwise? | Pieter van Dam | Open |
| OI-04 | Self-service kiosk hardware: reuse existing kiosks or replace them? | Ellen Ward | Open |

## Key contacts

| Name | Role |
|---|---|
| Margaret Doyle | Library Director — Project Sponsor |
| Hannah Mercer | Head of Public Services — Product Owner |
| Ellen Ward | Branch Manager, Central branch — Key user |
| Raj Malhotra | Finance Controller |
| Pieter van Dam | Data Protection Officer / Information Security |
| Tom Hollis | Business Analyst |
| Sara Lindqvist | Lead Architect |
| Jonas Becker | Backend Lead |
| Mira Okafor | Frontend Lead |
