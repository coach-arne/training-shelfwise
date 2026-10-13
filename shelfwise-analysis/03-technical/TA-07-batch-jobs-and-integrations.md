# TA-07 — Batch Jobs and Integrations

| Field | Value |
|---|---|
| Document ID | TA-07 |
| Version | 1.0 |
| Status | Approved |
| Owner | Sara Lindqvist, Lead Architect |
| Last updated | 2026-04-08 |
| Related | FA-02, FA-05, FA-06, FA-07, TA-02, TA-04 |

## 1. Scheduling

- Batch jobs run inside the backend application, scheduled with Spring scheduling.
- ShedLock (lock table in PostgreSQL) guarantees that a job runs on only one instance at a time.
- All times are local time (Europe/Brussels). All jobs must be finished before 06:00.

## 2. Nightly jobs

| ID | Job | Start | Description |
|---|---|---|---|
| J-01 | Fine accrual | 02:00 | For every overdue loan, if the current day is an opening day, adds the applicable daily rate to `loan.accrued_fine`. When the copy is returned, the accrued amount is converted into an overdue fine charge. |
| J-02 | Due date reminders and overdue notices | 02:30 | Selects loans for N-01 to N-04 according to FA-07 and sends the notifications through the Notify gateway. |
| J-03 | Lost declaration | 03:00 | Loans that are 42 calendar days past their due date: copy status becomes LOST, charges are created for the overdue fine, the replacement cost and the administration fee, and N-05 is sent. |
| J-04 | Reservation expiry | 03:15 | Holds whose hold period has ended: reservation becomes EXPIRED, N-08 is sent, the copy is assigned to the next reservation or becomes AVAILABLE. Also sends N-07 pickup reminders. |
| J-05 | Anonymisation | 03:30 | Anonymises loans returned more than 12 months ago, for members without the "Keep my reading history" opt-in and without open charges on the loan (FA-02 §7). |
| J-06 | Membership expiry reminders | 04:00 | Sends N-09 to members whose membership expires in 30 days. |

## 3. Failure handling

- All jobs are idempotent and can safely be restarted after a failure.
- Each job processes records in batches of 500 and commits per batch.
- A job failure triggers an alert to the operations team (TA-10 §5).
- Every run is logged with start time, end time, number of processed records and errors.

## 4. Integrations

### 4.1 PayBridge (online payments)

- The member portal redirects the member to the PayBridge hosted checkout page; card data never passes through Shelfwise.
- PayBridge calls the webhook (TA-05 §3.6) with the payment result. The webhook signature is verified with a shared secret.
- If no webhook is received within 30 minutes, the backend queries the payment status through the PayBridge status API.
- A daily settlement file from PayBridge is used by Finance for reconciliation outside Shelfwise.

### 4.2 Notify gateway (e-mail and SMS)

- The city's shared messaging service, called through its REST API.
- Shelfwise passes the template ID, the recipient and the template variables; templates are managed in the Notify gateway.
- E-mail and SMS are both supported. SMS is only used for members with SMS opt-in.
- Failed deliveries are retried three times with exponential backoff; the final status is stored in `notification_log`.

### 4.3 Keycloak

The backend uses the Keycloak admin API to create a member account when a member activates portal access.

### 4.4 LibTrack 4 migration

- One-off migration of members, titles, copies, open loans and open charges.
- Amounts are converted from LibTrack 4's format to Shelfwise's format during migration.
- Historic (returned) loans older than 12 months are not migrated, pending decision OI-03.
- Two rehearsals take place in the acceptance environment with masked data.
