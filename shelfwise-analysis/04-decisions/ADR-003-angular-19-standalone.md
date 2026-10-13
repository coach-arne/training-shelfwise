# ADR-003 — Angular 19 with Standalone Components and Signals; Two Applications

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-04-20 |
| Author | Mira Okafor |
| Deciders | Mira Okafor, Sara Lindqvist, Pieter van Dam |

## Context

The initial frontend design (TA-03 v1.0) was based on Angular 16 with NgModules and NgRx, in one application for both members and staff. Since then:

- Angular 16 is no longer supported; Shelfwise must go live on a supported version and stay on it for several years.
- Standalone components are the default in current Angular versions, and signals offer a simpler way to manage state for an application of this size than NgRx.
- The Data Protection Officer asked to separate the public member portal from the staff functions, so that staff screens and their code are never delivered to member browsers.

## Decision

1. The frontend uses **Angular 19** with **standalone components** (no NgModules).
2. Local and shared state is managed with **signals** and injectable services. **NgRx is not used.**
3. The frontend consists of **two applications** in one Angular workspace: `member-portal` and `staff-console`, sharing a library with common components and API clients.
4. Unit tests use **Jest**; end-to-end tests use **Playwright**.

## Consequences

- TA-03 must be rewritten according to this decision (action: Mira Okafor).
- Two Keycloak clients are needed (TA-06).
- Two static bundles are deployed.
- Less boilerplate than NgRx; the team needs a short training on signals.

## Alternatives considered

- **Stay on Angular 16 with NgRx:** rejected, unsupported version.
- **One application with role-based routing:** rejected, because staff code would be shipped to all members.
