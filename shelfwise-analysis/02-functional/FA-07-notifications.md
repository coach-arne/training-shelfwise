# FA-07 — Notifications

| Field | Value |
|---|---|
| Document ID | FA-07 |
| Version | 1.0 |
| Status | Approved |
| Owner | Tom Hollis, Business Analyst |
| Last updated | 2026-03-27 |
| Related | FA-04, FA-05, FA-06, TA-07 |

## 1. General rules

- Notifications are sent by e-mail. Members who opt in also receive N-01 and N-06 by SMS.
- Notifications for YOUTH members are sent to the guardian's e-mail address.
- Scheduled notifications are sent by the nightly batch run. N-06 and N-10 are sent immediately.
- If a member receives several scheduled notifications on the same day, they are combined into one message.
- Every notification is logged with date, type, channel and delivery status.

## 2. Notification catalogue

| ID | Notification | Trigger / timing | Channel |
|---|---|---|---|
| N-01 | Due date reminder | 2 days before the due date | E-mail, SMS if opted in |
| N-02 | First overdue notice | 1 day after the due date | E-mail |
| N-03 | Second overdue notice | 7 days after the due date | E-mail |
| N-04 | Third overdue notice | 21 days after the due date | E-mail |
| N-05 | Final notice — item declared lost | 30 days after the due date. The item is declared lost and the replacement cost and administration fee are charged (see FA-06 §4). | E-mail |
| N-06 | Reservation ready for pickup | When the copy is set ON_HOLD | E-mail, SMS if opted in |
| N-07 | Pickup reminder | Day 5 of the hold period | E-mail |
| N-08 | Reservation expired | When the hold period ends without pickup | E-mail |
| N-09 | Membership expiry reminder | 30 days before the membership expires | E-mail |
| N-10 | Payment receipt | When a payment is registered | E-mail |

## 3. Content

Every notification contains: the member's name, the titles concerned, the relevant dates, the accrued or charged amounts where applicable, and a link to the member portal. Templates are managed in the Notify gateway and approved by the Head of Public Services.

## 4. Notification preferences

Members can manage SMS opt-in and their contact details in the portal. E-mail notifications cannot be switched off, because overdue and lost notices are part of the lending conditions.
