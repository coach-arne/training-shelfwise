# TA-03 — Frontend Design (Angular)

| Field | Value |
|---|---|
| Document ID | TA-03 |
| Version | 1.0 |
| Status | Approved |
| Owner | Mira Okafor, Frontend Lead |
| Last updated | 2026-03-18 |
| Related | TA-01, TA-05, TA-06 |

## 1. Scope

This document describes the design of the Shelfwise web frontend.

## 2. Technology

| Item | Choice |
|---|---|
| Framework | Angular 16 |
| Language | TypeScript 5.1 |
| UI components | Angular Material 16 |
| State management | NgRx 16 (store, effects, entity) |
| Reactive programming | RxJS 7 |
| Authentication library | angular-oauth2-oidc |
| Unit tests | Karma and Jasmine |
| End-to-end tests | Cypress |

## 3. Application structure

There is a single Angular application, `shelfwise-web`, used by both members and staff. The available functions depend on the role of the logged-in user; route guards prevent access to staff screens for members.

The application is organised in NgModules:

| Module | Content |
|---|---|
| AppModule | Root module, routing, layout |
| CoreModule | Authentication, HTTP interceptors, error handling (imported once) |
| SharedModule | Reusable components, pipes and Material imports |
| MembersModule | Member search, registration, member details (lazy loaded) |
| LoansModule | Checkout, return, renewal, my loans (lazy loaded) |
| ReservationsModule | Reservations and holds (lazy loaded) |
| FinesModule | Charges, waivers, payments (lazy loaded) |
| ReportsModule | Reports (lazy loaded) |

## 4. State management

- Application state is held in the NgRx store, with a feature state per functional area (loans, charges, reservations).
- Effects call the backend services and dispatch success or failure actions.
- Lists use the NgRx entity adapter.
- Components select state through selectors and never call HTTP services directly.

## 5. Backend communication

- One Angular service per resource, using HttpClient.
- Base URL: `/rest`.
- Examples: loans `/rest/loans`, renewal `/rest/loans/renew`, charges `/rest/fines`, payments `/rest/payments`.
- An HTTP interceptor adds the access token and a correlation ID header to every request.

## 6. Authentication

Login via Keycloak using the authorisation code flow with PKCE. Tokens are kept in memory only; the session is refreshed silently. After login, the user's role determines the start page.

## 7. Fine display

The frontend never calculates fines. It always displays the accrued fine or the charge amount as returned by the backend, so that the portal, the staff console and the receipts show the same amount.

## 8. Accessibility and responsiveness

- WCAG 2.1 level AA.
- The member functions are fully usable on mobile devices; staff functions are designed for desktop screens at the desk.
