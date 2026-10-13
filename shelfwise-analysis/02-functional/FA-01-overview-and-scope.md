# FA-01 — Functional Overview and Scope

| Field | Value |
|---|---|
| Document ID | FA-01 |
| Version | 1.2 |
| Status | Approved |
| Owner | Tom Hollis, Business Analyst |
| Last updated | 2026-04-15 |
| Related | BUS-01, FA-02 to FA-09 |

## 1. Purpose

This document gives an overview of the functional analysis of Shelfwise: the actors, the functional areas, the scope of phase 1 and the general assumptions. Detailed requirements are described in FA-02 to FA-09.

## 2. Actors

| Actor | Description | Channel |
|---|---|---|
| Member | Registered library user | Member portal (web, responsive) |
| Guardian | Parent or guardian of a youth member | Receives notifications; no own portal role |
| Librarian | Desk staff at a branch | Staff console |
| Branch Manager | Responsible for a branch; handles escalations | Staff console |
| Administrator | Maintains system configuration and staff accounts | Staff console |
| PayBridge | External online payment provider | System integration |
| Notify gateway | City messaging service for e-mail and SMS | System integration |

## 3. Functional areas

| Document | Area | Summary |
|---|---|---|
| FA-02 | Members | Categories, registration, membership, entitlements, blocking, privacy |
| FA-03 | Catalogue and copies | Titles, copies, material types, copy statuses |
| FA-04 | Loans and renewals | Checkout, due dates, renewals, returns |
| FA-05 | Reservations | Reserving titles, queue, holds, reservation fee |
| FA-06 | Fines and payments | Overdue fines, lost items, waivers, payments |
| FA-07 | Notifications | Reminders and notices to members |
| FA-08 | Reporting | Operational and financial reports |
| FA-09 | Roles and permissions | Who may do what |

## 4. Scope of phase 1

### 4.1 In scope

- Member registration and membership management (FA-02)
- Title and copy management for lending purposes (FA-03)
- Checkout, return and renewal at the desk; renewal via the member portal (FA-04)
- Reservations and holds (FA-05)
- Fines, lost items, waivers, desk payments and online payments (FA-06)
- Notifications by e-mail and optional SMS (FA-07)
- Operational reporting (FA-08)
- Inter-library loans: requesting titles from partner libraries in the region (see FA-10)

### 4.2 Out of scope

- E-books and digital lending
- Acquisitions and purchasing
- Events and room booking
- Self-service kiosk hardware

## 5. General assumptions

1. Shelfwise is one system serving all six branches.
2. There is one library-wide calendar of closing days (all Sundays and public holidays). Branch-specific closures are not supported in phase 1.
3. All amounts are in euro.
4. The member portal and the staff console are available in English in phase 1.
5. Members, titles, copies, open loans and open charges are migrated from LibTrack 4 before go-live.

## 6. Change history of this document

| Version | Change |
|---|---|
| 1.0 | Initial version |
| 1.1 | Document map added |
| 1.2 | Scope section revised: inter-library loans added following feedback from branch managers |
