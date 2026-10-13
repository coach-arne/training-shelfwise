# Assumptions and choices

Things the specification (`summary.md`) leaves open, and how Shelfwise handles them. Each item can be changed in
one place; the file and rule are named.

## Scope

- Increments 1 and 2 of section 17 are implemented. Increment 3 is **not**: book drop (AC-LEN-11), online
  pre-registration, online membership renewal, closing a membership, reports R-03 to R-07 (AC-ROL-04), PDF
  output, the loan-limit and staff-account admin screens, and the audit trail. The data model leaves room for them
  (for example `loan_limit` is a table, reservation fees are ordinary charges).
- Loan limits (OD-01: ADULT 10, YOUTH 5, SENIOR 10) are rows in `loan_limit`; there is no screen to change them.

## Accounts and login

- Members log in with their **card number** and a password. All seed accounts use the password `shelfwise`.
  A newly registered member gets a random temporary password that is shown once to the librarian.
- Staff log in with their user name (`admin`, `manager.cen`, `librarian.cen`, `librarian.riv`).
- In training mode (`SHELFWISE_TRAINING_MODE=true`) the login page also lists the seed accounts for one-click login.
- Sessions are stateless JWTs (HS256, 12 hours by default, `shelfwise.jwt-expiry-hours`). The secret comes from `JWT_SECRET`.
- `librarian.cen` is linked to member Sam Stone (S-19) for the own-account waiver rule (AC-FIN-22).

## Dates and daily processing

- The business date is the real date in production and `system_state.business_date` in training mode.
- Moving the business date to X runs the daily processing through X-1 (the day that is left has been processed after
  closing time). "Run daily processing now" runs it for the current business date X. Moving backward is refused;
  a reset to the seed data restores 2026-03-16.
- In production the missing runs are caught up at start-up and the scheduler runs once per day at 22:00 Brussels time (`shelfwise.daily-processing-cron`).
- One transaction per processed date; a failing day does not roll back earlier days.
- N-09 (membership expiry reminder) is sent 30 days before the "valid until" date, which is the last valid day.

## Members and membership

- `membership_end` is the last valid day (inclusive). A membership "expired yesterday" has an end date of yesterday.
- The category is set at registration and re-evaluated at membership renewal on the renewal date (AC-MEM-07);
  it does not change on a birthday in between.
- On an early renewal the new period starts on the day after the old end (BR-MEM-07); the stored start date is that day.
- Membership fees are recorded in `membership_fee_payment`, not as charges (section 6.3).
- A YOUTH member needs a guardian: an existing ADULT/SENIOR member, or a name and e-mail address.
- A registration that looks like an existing member is refused with `POSSIBLE_DUPLICATE` (F-MEM-02) until the librarian confirms it.

## Lending, routing and reservations

- A copy returned for repair-like reasons, a hold that expires, or a hold that is cancelled, with nobody waiting,
  becomes AVAILABLE at the branch where it is; it does not travel home.
- A reservation is refused when the title has no copies in circulation (every copy lost or withdrawn).
- An AVAILABLE copy at another branch is not assigned automatically to a new reservation. The reservation waits for
  a return or arrival (section 9.1 only forbids reserving when a copy is available at the pickup branch).
- The reservation queue is ordered by placement time, then id.
- Online renewals appear in report R-01 with channel ONLINE and no branch, so only in the all-branches view. R-01 is scoped by the branch of the event, R-02 by the branch of checkout.
- Librarians cannot choose a branch in the reports; the server scopes them to their own branch (AC-ROL-01).

## Finance

- All money is `BigDecimal`, scale 2, rounding half up. The fine is rounded once per copy (BR-FIN-05).
- The SENIOR rate is the ADULT rate with 50% discount; only ADULT and YOUTH rows can be maintained (F-ADM-01).
  Only rates that start in the future can be added or removed.
- Cancelling a charge (lost copy returned, reservation cancelled by the library) records a CANCELLATION transaction and,
  when something was paid, a REFUND transaction. The refund is a recorded amount owed back; no cash movement is modelled.
- If charges changed between starting and completing an online payment, only the charges that are still open are paid and
  the amount actually allocated is recorded.
- An online payment session is idempotent: a pessimistic row lock plus a unique provider reference means that the same
  success reported twice is processed once (AC-PAY-05).

## Notifications

- Nothing is sent. Every notification is a row in the outbox with date, type, channel, recipient and status
  "SENT (simulated)". Scheduled notices of one run are combined per recipient e-mail address (AC-NOT-05).
- N-01 is also sent by SMS to opted-in members; N-06 as well. YOUTH members never receive SMS and their e-mails go to the guardian.

## Technical

- The REST API returns HTTP 422 with `{code, ruleId, message}` for refused business actions, so that every refusal
  can be traced to a BR-/F- id.
- A refused check inside a read-only service must not mark the caller's transaction rollback-only; `MemberStatusService`
  is therefore annotated with `noRollbackFor = BusinessRuleException.class`. (Found by AC-MEM-04.)
- The front-end uses Angular signals for state (module-level signals in `src/app/store`), plain functions for API calls
  and helpers, and Angular Material. Jotai was not used because the app is Angular.
