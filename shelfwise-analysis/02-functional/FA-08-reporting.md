# FA-08 — Reporting

| Field | Value |
|---|---|
| Document ID | FA-08 |
| Version | 1.0 |
| Status | Approved |
| Owner | Tom Hollis, Business Analyst |
| Last updated | 2026-04-01 |
| Related | FA-06, FA-09 |

## 1. Report catalogue

| ID | Report | Content | Frequency | Primary audience |
|---|---|---|---|---|
| R-01 | Daily circulation | Checkouts, returns and renewals per branch and material type | Daily, on screen | Librarians (own branch), Branch Managers |
| R-02 | Overdue items | Open overdue loans with member, copy, due date and accrued fine | On demand | Librarians (own branch), Branch Managers |
| R-03 | Charges and payments summary | All charges by type, payments by method, waivers and outstanding balance per month | Monthly, e-mailed as CSV to the Finance Controller | Branch Managers, Finance Controller |
| R-04 | Waivers | Every waiver with amount, reason, staff member and branch | Monthly and on demand | Branch Managers |
| R-05 | Lost items | Copies declared lost, replacement costs charged and recovered | Monthly | Branch Managers |
| R-06 | Popular titles | Most borrowed titles per period and branch, based on aggregated data only | Monthly | Branch Managers |
| R-07 | Reservation queues | Titles with the longest reservation queues | Weekly | Branch Managers |

## 2. General requirements

- Reports can be exported as CSV and PDF.
- Reports never show reading history of individual members, except R-02 (open overdue loans), which is needed for follow-up.
- R-06 uses anonymised loan data and therefore covers all loans, including anonymised ones.
- Access rights to reports are described in FA-09 and may be broader than the primary audience.
