# TA-08 — Non-Functional Requirements

| Field | Value |
|---|---|
| Document ID | TA-08 |
| Version | 1.0 |
| Status | Approved |
| Owner | Sara Lindqvist, Lead Architect |
| Last updated | 2026-03-10 |
| Related | TA-01, TA-06, TA-09, TA-10 |

## 1. Capacity and sizing

Sizing is based on the LibTrack 4 usage statistics in the library's annual report 2019:

| Metric | Value |
|---|---|
| Active members | 25,000 |
| Branches | 3 |
| Copies | 120,000 |
| Loans per year | 300,000 |
| Peak concurrent staff users | 60 |
| Peak concurrent portal users | 200 |

The infrastructure must support 20% growth on these figures without architectural changes.

## 2. Performance

| Operation | Target (95th percentile) |
|---|---|
| API read requests | < 500 ms |
| API write requests (checkout, return, renewal) | < 1 s |
| Catalogue search | < 1 s |
| Accrued fine calculation | < 300 ms |
| Nightly batch jobs | All finished before 06:00 |

## 3. Availability

- 99.5% availability during opening hours (Monday to Saturday, 08:00 – 20:00).
- Planned maintenance on Sundays, announced at least five days in advance.

## 4. Scalability

The backend runs with at least two instances behind the OpenShift router and can be scaled horizontally. The database is a managed service of city IT with a standby replica.

## 5. Backup and recovery

- Recovery point objective (RPO): 15 minutes.
- Recovery time objective (RTO): 4 hours.

## 6. Usability and accessibility

- WCAG 2.1 level AA for the member portal and the staff console.
- The member portal is responsive and usable on smartphones.
- A checkout at the desk requires no more than two scans and one confirmation.

## 7. Browser support

The last two versions of Chrome, Edge, Firefox and Safari.

## 8. Security

See TA-06.
