# FA-05 — Reservations

| Field | Value |
|---|---|
| Document ID | FA-05 |
| Version | 1.0 |
| Status | Approved |
| Owner | Tom Hollis, Business Analyst |
| Last updated | 2026-03-27 |
| Related | FA-03, FA-04, FA-06, FA-07 |

## 1. Placing a reservation

**UC-RES-01 — Reserve a title** (member portal or desk)

- Reservations are placed on a **title**, not on a specific copy (BR-RES-01).
- The member chooses a pickup branch.
- A member can reserve a title when no copy of that title is available at the chosen pickup branch.
- A member can have at most **5 active reservations** (BR-RES-04). Active means: waiting or on hold.
- Blocked members and members with an expired membership cannot place reservations.

## 2. Reservation fee

- A reservation fee of **€0.50** applies to ADULT and SENIOR members. Reservations are free for YOUTH members (BR-RES-03).
- The fee is charged when the reservation is placed and is added to the member's account balance as a charge.
- The fee is not refunded when the member cancels the reservation or does not collect the copy.
- The fee is refunded (or cancelled if unpaid) when the library cancels the reservation, for example because the title is withdrawn.

## 3. Queue

- Reservations for a title form a queue in order of creation (first in, first out).
- When a copy of the title is returned or becomes available, it is assigned to the first reservation in the queue.
- If the copy is at the pickup branch, it gets status ON_HOLD. Otherwise it gets status IN_TRANSIT and becomes ON_HOLD on arrival.

## 4. Hold period

- The member is notified when the copy is ready for pickup (FA-07, N-06).
- The copy stays on hold for **7 days** after the notification (BR-RES-02). A reminder is sent on day 5 (N-07).
- If the copy is not collected within 7 days, the reservation expires (N-08) and the copy is assigned to the next reservation in the queue or goes back to AVAILABLE.

## 5. Cancellation

Members can cancel a reservation at any time before collection, in the portal or at the desk.

## 6. Interaction with renewals

A waiting reservation on a title prevents renewal of loans of that title (FA-04 §4).
