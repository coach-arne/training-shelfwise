# FA-02 — Members

| Field | Value |
|---|---|
| Document ID | FA-02 |
| Version | 1.1 |
| Status | Approved |
| Owner | Tom Hollis, Business Analyst |
| Last updated | 2026-04-10 |
| Related | FA-04, FA-06, FA-09, TA-06 |

## 1. Member categories

| Category | Age | Notes |
|---|---|---|
| YOUTH | 0 – 17 | Must be registered with a guardian |
| ADULT | 18 – 64 | |
| SENIOR | 65 and older | |

The category is determined by the member's age on the date of registration and is re-evaluated automatically at every membership renewal (BR-MEM-01).

## 2. Registration

**UC-MEM-01 — Register a member at the desk**

1. The librarian searches for the person to avoid duplicate registrations (name and date of birth).
2. The librarian records name, date of birth, address, e-mail address and optionally a mobile phone number, after checking an identity document.
3. For a youth member, the librarian links a guardian. The guardian is either an existing adult member or is recorded with name and e-mail address only.
4. The system assigns a member card number and prints the library card.
5. The membership fee is paid at the desk (see §3).

**UC-MEM-02 — Pre-register online**

A person can pre-register via the member portal. The registration is completed at a desk after the identity check. Pre-registrations that are not completed within 30 days are deleted.

## 3. Membership validity and fees

- A membership is valid for 12 months and can be renewed at the desk or via the portal.
- Annual membership fee (BR-MEM-04): ADULT €12.00, YOUTH free, SENIOR €6.00.
- Membership fees are paid when registering or renewing. They are not added to the account balance.
- With an expired membership a member cannot borrow, renew or reserve, but can still return items, pay charges and view the account.

## 4. Borrowing entitlements

Maximum number of simultaneous loans per member (BR-MEM-02):

| Category | Maximum loans |
|---|---|
| ADULT | 10 |
| YOUTH | 5 |
| SENIOR | 10 |

The maximum number of active reservations is described in FA-05.

## 5. Account balance and blocking

- The account balance is the sum of all unpaid charges (see FA-06 §7).
- A member is **blocked** when the outstanding balance is **€5.00 or more**, or when the membership has expired (BR-MEM-03).
- A blocked member cannot borrow, renew or place reservations. Returning items, paying and viewing the account remain possible.
- The block is lifted automatically as soon as the balance drops below €5.00 and the membership is valid.

## 6. Self-service in the member portal

Members can:

- view current loans with due dates and the fine accrued so far;
- view their loan history (if kept, see §7);
- view charges and the account balance, and pay online (FA-06 §6);
- renew loans (FA-04 §4) and manage reservations (FA-05);
- update e-mail address, phone number and notification preferences.

## 7. Privacy and loan history

This section was added in version 1.1 after input from the Data Protection Officer.

- Loan records are **anonymised 12 months after the return date**: the link between the loan and the member is removed (BR-MEM-05). Anonymised loans remain available for statistics.
- A member can opt in to "Keep my reading history". In that case loans are not anonymised as long as the opt-in is active.
- When a member withdraws the opt-in, all returned loans older than 12 months are anonymised during the next nightly run.
- Loans with unpaid charges are not anonymised until those charges are settled.
- Charges and payments are retained for 7 years for accounting purposes.

## 8. Closing a membership

A member can ask to close the membership and delete personal data, provided there are no loans in progress and the account balance is zero. Financial records are kept as described in §7.
