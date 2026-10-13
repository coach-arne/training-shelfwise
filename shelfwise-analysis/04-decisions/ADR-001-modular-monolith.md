# ADR-001 — Modular Monolith

| Field | Value |
|---|---|
| Status | Accepted |
| Date | 2026-03-04 |
| Author | Sara Lindqvist |
| Deciders | Sara Lindqvist, Jonas Becker, Mira Okafor, Hannah Mercer |

## Context

Shelfwise is built and maintained by a small team (four to six developers) for one organisation. The expected load is moderate and predictable (opening hours of six branches plus the member portal). The functional areas — members, catalogue, lending, reservations, fines, payments — are closely related and many use cases touch several of them in one transaction (for example: a return closes a loan, creates a charge and assigns the copy to a reservation).

## Decision

Shelfwise is built as a **modular monolith**: one Spring Boot application, deployed as a single unit, with explicit internal modules. Modules communicate only through public application services. Module boundaries are enforced with architecture tests.

## Consequences

- One deployment pipeline and one database; simple transactions across modules.
- Clear module boundaries keep the option open to extract a module later (for example notifications).
- Discipline is needed to prevent modules from using each other's internals; this is checked automatically.

## Alternatives considered

- **Microservices:** rejected. Operational complexity and distributed transactions are not justified for this team size and load.
- **Unstructured monolith:** rejected. Risk of high coupling and a hard-to-maintain code base.
