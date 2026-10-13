# FA-09 — Roles and Permissions

| Field | Value |
|---|---|
| Document ID | FA-09 |
| Version | 1.1 |
| Status | Approved |
| Owner | Tom Hollis, Business Analyst |
| Last updated | 2026-04-22 |
| Related | FA-06, FA-08, TA-06 |

## 1. Purpose

This document describes which role may perform which function. The technical implementation of authentication and authorisation is described in TA-06.

## 2. Roles

| Role | Description |
|---|---|
| MEMBER | A registered member using the member portal |
| LIBRARIAN | Desk staff, linked to one branch |
| BRANCH_MANAGER | Manager of a branch; has all librarian permissions plus escalation and reporting rights |
| ADMINISTRATOR | Maintains configuration and staff accounts; does not perform lending or financial transactions |

## 3. Permission matrix

| Function | MEMBER | LIBRARIAN | BRANCH_MANAGER | ADMINISTRATOR |
|---|---|---|---|---|
| View own account, loans and charges | Yes | — | — | — |
| Renew own loans online | Yes | — | — | — |
| Place and cancel own reservations | Yes | — | — | — |
| Pay online | Yes | — | — | — |
| Register and edit members | — | Yes | Yes | No |
| Checkout, return and renew at the desk | — | Yes | Yes | No |
| Place and cancel reservations for members | — | Yes | Yes | No |
| Maintain titles and copies | — | Yes | Yes | No |
| Register desk payments | — | Yes | Yes | No |
| Waive charges | — | Up to €2.00 per charge | Yes, any amount | No |
| Reports R-01 and R-02 | — | Own branch | All branches | All branches |
| Reports R-03 to R-07 | — | No | Yes | Yes |
| Maintain fine tariffs and closing days | — | No | No | Yes |
| Manage staff accounts and roles | — | No | No | Yes |

## 4. Waiver rules

These rules were added in version 1.1 following the fines workshop of 2 April 2026.

- A reason is mandatory for every waiver.
- A librarian may waive at most **€2.00 per charge**. Larger waivers require a Branch Manager.
- A Branch Manager may waive any amount.
- Staff members may never waive charges on their own member account.
- All waivers appear in report R-04.

## 5. Segregation of duties

The ADMINISTRATOR role does not perform lending or financial transactions. A person who needs both responsibilities receives two separate accounts.
