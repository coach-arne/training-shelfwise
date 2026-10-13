# Shelfwise — Functional Specification for Implementation

| Field | Value |
|---|---|
| Document | Consolidated functional specification |
| Version | 1.0 |
| Date | 2026-10-13 |
| Based on | Shelfwise analysis pack v1.3, including all decisions up to the steering committee of 2026-05-13 |
| Scope | Functional behaviour only. Technology, architecture, data design and interfaces are out of scope for this document. |

---

## 0. Read this first (instructions for the implementing agent)

1. **This document is the single source of truth for functional behaviour.** Where the analysis pack and this document differ, this document prevails.
2. **It describes what the system does, not how.** Technology choices, architecture and data design are made by the development team. If the project rules or your instructions specify a technology stack, follow those.
3. **Use the IDs.** Features (`F-…`), business rules (`BR-…`) and acceptance examples (`AC-…`) have stable IDs. Reference them in tests, commit messages and pull request descriptions so that every requirement can be traced to its implementation.
4. **Implement in increments** (§17). Only build the increments you have been asked to build.
5. **Do not invent behaviour.** If something is not specified, ask. If you are told to proceed anyway, choose the simplest reasonable behaviour and add it to a written list of assumptions that you report at the end.
6. **Open decisions** (§18) have provisional values. Implement the provisional value and keep it easy to change.
7. **Appendix C contains training exercises. They are not part of the implementation scope.** Do not implement them unless explicitly asked.

---

## 1. Context and scope

### 1.1 Context

Northwick Public Library (NPL) is a fictional public library with six branches and about 85,000 active members. Shelfwise is its new lending system. It replaces a legacy system called LibTrack 4.

| Branch | Code |
|---|---|
| Central | CEN |
| Riverside | RIV |
| Eastgate | EAS |
| Millbrook | MIL |
| Harbour | HAR |
| Westfield | WES |

There are two user-facing channels:

- the **member portal**, used by members (and anonymous visitors for catalogue search);
- the **staff console**, used by librarians, branch managers and administrators.

### 1.2 In scope

- Members: registration, categories, membership, entitlements, blocking, self-service, privacy
- Catalogue: titles, copies, copy statuses, search and availability
- Lending: checkout, due dates, renewals, returns, transfers between branches
- Reservations: placing, queue, holds, reservation fee
- Fines: overdue fines, lost items, waivers
- Payments: desk payments, online payments, account balance
- Notifications to members
- Operational reports
- Administration: tariffs, closing days, loan limits, staff accounts
- Training support functions (§15)

### 1.3 Out of scope

- Inter-library loans (phase 2)
- E-books and digital lending
- Acquisitions and purchasing
- Events and room booking
- Self-service kiosks
- Migration of data from LibTrack 4 (seed data is used instead, Appendix B)

---

## 2. Terminology

| Term | Meaning |
|---|---|
| Member | A person registered with the library. Never use "patron" or "customer". |
| Member category | YOUTH, ADULT or SENIOR (§6.1). |
| Guardian | Parent or guardian of a youth member. Receives the youth member's notifications. |
| Title | The description of a work: title, authors, publisher, year, ISBN (optional), material type. |
| Copy | One physical item of a title, identified by a unique barcode. |
| Material type | BOOK, AUDIOVISUAL or MAGAZINE. Determines loan period, fine rate and default replacement cost. |
| Home branch | The branch that owns a copy. |
| Loan | The lending of one copy to one member, from checkout until it is returned or declared lost. |
| Due date | The last day on which a loan can be returned without being overdue. |
| Renewal | Extending the due date of an open loan. |
| Reservation | A request by a member for a title, to be picked up at a chosen branch. |
| Hold | A copy set aside at a branch for the member whose reservation is first in the queue. |
| Opening day | Any day that is not a closing day. |
| Closing day | Every Sunday and every public holiday. |
| Chargeable day | An opening day on which a loan is overdue. |
| Charge | An amount a member owes: overdue fine, replacement cost, administration fee or reservation fee. |
| Account balance | The sum of the outstanding amounts of all charges of a member. |
| Blocked | A member who may not borrow, renew or reserve (§6.5). |
| Waiver | Cancelling all or part of a charge by authorised staff. |
| Business date | The date the system considers to be "today" (§5). |
| Daily processing | The once-per-day run that handles all time-based events (§5.2). |

---

## 3. Users, roles and permissions

### 3.1 Roles

| Role | Description |
|---|---|
| Anonymous visitor | Not logged in. Can only search the catalogue. |
| MEMBER | A member logged in to the member portal. Can only see and act on their own data. |
| LIBRARIAN | Desk staff, linked to one branch. |
| BRANCH_MANAGER | Manager of one branch. Has all librarian permissions, plus unlimited waivers and all reports. |
| ADMINISTRATOR | Maintains configuration and staff accounts. Does not perform lending or financial transactions. |

A person who needs both administrator and lending permissions gets two separate accounts. Staff members can also be members; their member account is separate from their staff account, but the staff account records which member account belongs to the same person (used for the own-account rule below).

### 3.2 Permission matrix

| Function | Visitor | MEMBER | LIBRARIAN | BRANCH_MANAGER | ADMINISTRATOR |
|---|---|---|---|---|---|
| Search catalogue and see availability | Yes | Yes | Yes | Yes | Yes |
| Pre-register online | Yes | — | — | — | — |
| View own account, loans, charges, history | — | Yes | — | — | — |
| Update own contact details and preferences | — | Yes | — | — | — |
| Renew own loans | — | Yes | — | — | — |
| Place and cancel own reservations | — | Yes | — | — | — |
| Pay online | — | Yes | — | — | — |
| Renew own membership online | — | Yes | — | — | — |
| Register and edit members | — | — | Yes | Yes | No |
| Checkout, return and renew at the desk | — | — | Yes | Yes | No |
| Place and cancel reservations for members | — | — | Yes | Yes | No |
| Maintain titles and copies | — | — | Yes | Yes | No |
| Register desk payments | — | — | Yes | Yes | No |
| Waive charges | — | — | Up to €2.00 per charge (BR-FIN-08) | Any amount | No |
| Reports R-01 and R-02 | — | — | Own branch only | All branches | All branches |
| Reports R-03 to R-07 | — | — | No | Yes | Yes |
| Maintain fine tariffs, closing days and loan limits | — | — | No | No | Yes |
| Manage staff accounts and roles | — | — | No | No | Yes |
| Training support functions (§15) | — | — | No | No | Yes |

No staff member may waive a charge on their own member account, regardless of role.

---

## 4. Parameters

These values are used throughout the specification. "Admin" means the administrator can change the value in the staff console; "Fixed" means the value does not change in phase 1.

| Parameter | Value | Changeable |
|---|---|---|
| Loan period BOOK / AUDIOVISUAL / MAGAZINE | 21 / 7 / 14 days | Fixed |
| Maximum simultaneous loans ADULT / YOUTH / SENIOR | 10 / 5 / 10 | Admin (YOUTH value: see OD-01) |
| Maximum renewals per loan | 2 | Fixed |
| Maximum active reservations per member | 5 | Fixed |
| Hold period | 7 days | Fixed |
| Pickup reminder | 5 days after the pickup notification | Fixed |
| Reservation fee ADULT / SENIOR / YOUTH | €0.50 / €0.50 / €0.00 | Fixed |
| Daily fine rates | See §10.2 | Admin (with valid-from date) |
| Senior discount on overdue fines | 50% | Fixed |
| Grace period | 1 opening day | Fixed |
| Maximum overdue fine per copy | Lower of €10.00 and the copy's replacement cost | Fixed |
| Lost declaration | 42 calendar days after the due date | Fixed |
| Administration fee for a lost copy | €5.00 | Fixed |
| Refund window for a returned lost copy | 90 days after the lost declaration | Fixed |
| Blocking threshold | Account balance of €5.00 or more | Fixed |
| Librarian waiver limit | €2.00 in total per charge | Fixed |
| Membership validity | 12 months | Fixed |
| Membership fee ADULT / YOUTH / SENIOR | €12.00 / €0.00 / €6.00 | Fixed |
| Default replacement cost BOOK / AUDIOVISUAL / MAGAZINE | €25.00 / €20.00 / €6.00 | Fixed |
| Anonymisation of returned loans | 12 months after the return date | Fixed |
| Expiry of incomplete online pre-registrations | 30 days | Fixed |
| Membership expiry reminder | 30 days before expiry | Fixed |
| Closing days | Sundays plus public holidays (§5.1) | Admin (public holidays) |

---

## 5. Calendar, business date and daily processing

### 5.1 Calendar

- Every Sunday is a closing day.
- Public holidays are closing days. The administrator maintains the list. Initial list for 2026:

| Date | Holiday |
|---|---|
| 2026-01-01 | New Year's Day |
| 2026-04-06 | Easter Monday |
| 2026-05-01 | Labour Day |
| 2026-05-14 | Ascension Day |
| 2026-05-25 | Whit Monday |
| 2026-07-21 | National Day |
| 2026-08-15 | Assumption Day |
| 2026-11-01 | All Saints' Day |
| 2026-11-11 | Armistice Day |
| 2026-12-25 | Christmas Day |

- The calendar is the same for all branches. Branch-specific closures are not supported.
- All dates in this specification are calendar dates in the library's local time zone.

### 5.2 Daily processing

- Daily processing runs **once for every calendar day**, including closing days, **after closing time**. The run for date D handles all events that are due on date D.
- Daily processing handles: due date reminders and overdue notices (§12), lost declarations (§10.8), hold reminders and hold expiry (§9.4), anonymisation (§6.7), expiry of incomplete pre-registrations (§6.2) and membership expiry reminders (§12).
- If daily processing for one or more days has not run (for example because the business date was moved forward, §15), the missing runs are executed in date order before anything else happens.

### 5.3 Business date

All functions use the **business date** as "today". In production the business date equals the real date. In training mode the administrator can change it (§15).

---

## 6. Members (F-MEM)

### 6.1 Member categories — F-MEM-01

| Category | Age |
|---|---|
| YOUTH | 0 – 17 |
| ADULT | 18 – 64 |
| SENIOR | 65 and older |

- **BR-MEM-01** The category is determined by the member's age on the registration date and re-evaluated at every membership renewal.
- **BR-MEM-06** Between two renewals the category does not change, even if the member's age passes a boundary.

### 6.2 Registration

**F-MEM-02 — Register a member at the desk**

1. The librarian checks whether the person is already registered (search by name and date of birth) and warns about a possible duplicate.
2. The librarian records first name, last name, date of birth, street, postal code, city, e-mail address and, optionally, a mobile phone number, after checking an identity document.
3. A YOUTH member must be linked to a guardian: either an existing ADULT or SENIOR member, or a guardian recorded with name and e-mail address only.
4. The system assigns a unique card number and the category (BR-MEM-01).
5. The membership fee is paid at the desk (§6.3). The membership is valid from the registration date.

**F-MEM-03 — Pre-register online**

A visitor can pre-register in the member portal with the same data. The pre-registration is completed at a desk (identity check, fee). Pre-registrations that are not completed within 30 days are deleted by daily processing.

### 6.3 Membership validity and fees — F-MEM-04

- A membership is valid for 12 months.
- **BR-MEM-04** Annual fee: ADULT €12.00, YOUTH free, SENIOR €6.00. The fee is paid when registering or renewing. Membership fees are not charges and are not part of the account balance.
- The membership can be renewed at the desk, or in the member portal with online payment of the fee.
- **BR-MEM-07** On renewal, the new period starts on the day after the current period ends. If the membership has already expired, the new period starts on the renewal date.
- With an expired membership a member cannot borrow, renew loans or place reservations, but can return copies, pay charges and view the account.

### 6.4 Borrowing entitlements — F-MEM-05

- **BR-MEM-02** Maximum simultaneous open loans: ADULT 10, YOUTH 5 (provisional, OD-01), SENIOR 10.
- **BR-RES-04** Maximum active reservations (waiting or on hold): 5.

### 6.5 Account balance and blocking — F-MEM-06

- The account balance is the sum of the outstanding amounts of all charges of the member, including reservation fees (OD-02).
- **BR-MEM-03** A member is **blocked** when the account balance is **€5.00 or more**, or when the membership has expired.
- A blocked member cannot borrow, renew or place reservations. Returning copies, paying and viewing the account remain possible.
- The block is lifted immediately when the balance drops below €5.00 and the membership is valid. There is no separate unblock action.
- Staff screens and the member portal always show whether a member is blocked and why.

### 6.6 Self-service in the member portal — F-MEM-07

Members can:

- see current loans with due dates, the number of renewals left, and the fine accrued so far (§10.7);
- see their loan history, as far as it has not been anonymised (§6.7);
- see their charges, outstanding amounts and account balance, and pay online (§11);
- renew loans (§8.4) and place or cancel reservations (§9);
- update e-mail address, phone number and SMS opt-in;
- switch the "Keep my reading history" opt-in on or off;
- renew their membership (§6.3).

### 6.7 Privacy and loan history — F-MEM-08

- **BR-MEM-05** A returned loan is **anonymised 12 months after its return date**: the link between the loan and the member is removed. Anonymised loans remain available for statistics (R-06).
- Anonymisation happens in the daily processing of the date on which the 12 months have passed.
- If the member has opted in to "Keep my reading history", their loans are not anonymised while the opt-in is active.
- When a member withdraws the opt-in, all their returned loans older than 12 months are anonymised in the next daily processing run.
- A loan that has a charge with an outstanding amount is not anonymised until that charge is settled (paid or waived).
- Charges and payments are kept for 7 years for accounting purposes.

### 6.8 Closing a membership — F-MEM-09

A member can ask at the desk to close the membership and delete their personal data. This is only possible if the member has no open loans and an account balance of zero. Charges and payments are kept as described in §6.7, without the member's contact details.

---

## 7. Catalogue (F-CAT)

### 7.1 Titles and copies — F-CAT-01

- A **title** has: title, authors, publisher, publication year, ISBN (optional) and material type.
- A **copy** has: unique barcode, title, home branch, current branch, shelf mark, status, replacement cost (optional) and acquisition date.
- If a copy has no replacement cost, the default for its material type applies: BOOK €25.00, AUDIOVISUAL €20.00, MAGAZINE €6.00.
- Librarians and branch managers can create and edit titles and copies.
- Loans and holds always concern a **copy**. Reservations are placed on a **title**.

### 7.2 Material types — F-CAT-02

| Material type | Examples | Loan period | Default replacement cost |
|---|---|---|---|
| BOOK | Novels, non-fiction, comics, graphic novels | 21 days | €25.00 |
| AUDIOVISUAL | DVD, Blu-ray, audio CD | 7 days | €20.00 |
| MAGAZINE | Back issues of periodicals | 14 days | €6.00 |

### 7.3 Copy statuses — F-CAT-03

| Status | Meaning |
|---|---|
| AVAILABLE | On the shelf at its current branch and available for loan |
| ON_LOAN | Borrowed by a member |
| ON_HOLD | Set aside at a branch for the member whose reservation is first in the queue |
| IN_TRANSIT | Being transferred to another branch |
| LOST | Declared lost (§10.8) |
| DAMAGED | Temporarily withdrawn for repair |
| WITHDRAWN | Permanently removed from the collection |

Status transitions:

| From | To | Trigger |
|---|---|---|
| AVAILABLE | ON_LOAN | Checkout |
| ON_HOLD | ON_LOAN | Checkout by the member for whom the copy is held |
| ON_LOAN | AVAILABLE / ON_HOLD / IN_TRANSIT | Return (routing rules in §8.6) |
| IN_TRANSIT | AVAILABLE / ON_HOLD | Check-in at the destination branch (§8.6) |
| ON_HOLD | ON_HOLD / AVAILABLE | Hold expires or is cancelled (§9.4) |
| ON_LOAN | LOST | Lost declaration in daily processing (§10.8) |
| LOST | AVAILABLE / ON_HOLD / IN_TRANSIT | The lost copy is returned (§10.9) |
| AVAILABLE | DAMAGED / WITHDRAWN | Manual action by staff |
| DAMAGED | AVAILABLE / WITHDRAWN | Manual action by staff |

- **BR-CAT-01** Staff can set a copy to DAMAGED or WITHDRAWN only when it is AVAILABLE (or DAMAGED, for WITHDRAWN). A copy that is on loan, on hold or in transit cannot be withdrawn directly.
- **BR-RES-07** When a copy becomes AVAILABLE for any reason (return, arrival, repair, a new copy being added), the system first checks whether a reservation is waiting for the title (§9.3).
- When the last copy of a title that is not WITHDRAWN or LOST is withdrawn, all reservations on that title are cancelled by the library, and their reservation fee charges are cancelled, with a refund of any amount already paid (BR-RES-03).

### 7.4 Catalogue search and availability — F-CAT-04

- Visitors and members can search by title, author, ISBN and keyword.
- Each result shows, per branch, the number of AVAILABLE copies, and the number of reservations waiting for the title.
- Staff can additionally search by barcode and see the status and current branch of every copy.

---

## 8. Lending (F-LEN)

### 8.1 Checkout — F-LEN-01

At the desk the librarian scans the member card and then the copy barcode. The system checks, in this order:

1. the membership is valid;
2. the member is not blocked (balance below €5.00, BR-MEM-03);
3. the maximum number of simultaneous loans is not reached (BR-MEM-02);
4. the copy is AVAILABLE, or ON_HOLD for this member.

If a check fails, the checkout is refused and the reason is shown. **BR-LOA-07** None of these checks can be overridden, by any role.

If the checkout succeeds:

- the loan is created with the business date as checkout date and the due date (§8.3);
- the copy becomes ON_LOAN;
- if the copy was ON_HOLD for this member, the reservation becomes COLLECTED;
- the member receives a receipt (printed or by e-mail) listing all open loans and their due dates.

### 8.2 Loan periods — F-LEN-02

**BR-LOA-01** BOOK 21 days, AUDIOVISUAL 7 days, MAGAZINE 14 days.

### 8.3 Due date — F-LEN-03

- Due date = checkout date + loan period.
- **BR-LOA-02** If the due date falls on a closing day, it moves to the next opening day.

### 8.4 Renewal — F-LEN-04

A member can renew a loan in the member portal; a librarian can renew it at the desk.

- **BR-LOA-03** A loan can be renewed at most **2 times**.
- **BR-LOA-04** New due date = **current due date** + loan period, then moved to the next opening day if it falls on a closing day (BR-LOA-02). The date on which the renewal is done does not matter.
- **BR-LOA-05** A renewal is refused when:
  - the title has one or more WAITING reservations without an assigned copy (reservations that are on hold, or whose copy is already in transit to them, do not count);
  - the member is blocked or the membership has expired;
  - the loan is overdue;
  - the loan has already been renewed 2 times.
- If a renewal is refused, the reason is shown to the member or the librarian.
- The same rules apply in the portal and at the desk.

### 8.5 Return — F-LEN-05

- **BR-LOA-06** A copy can be returned at **any branch**.
- The return date is the business date on which the copy is checked in.
- **Book drop:** copies dropped in the after-hours book drop are checked in on the next opening day. The librarian marks them as book-drop returns; their return date is the last opening day before the check-in date.
- On return, the system:
  1. closes the loan with the return date;
  2. calculates the overdue fine (§10) and, if it is more than €0.00, creates an overdue fine charge;
  3. routes the copy (§8.6);
  4. gives the member a return receipt showing any fine charged.

### 8.6 Routing of returned copies — F-LEN-06

When a copy is checked in (on return, or on arrival at a branch after a transfer):

| Situation | Result |
|---|---|
| The first WAITING reservation without an assigned copy has the current branch as pickup branch | Copy becomes ON_HOLD for that reservation; pickup notification N-06 is sent |
| The first WAITING reservation without an assigned copy has another pickup branch | Copy becomes IN_TRANSIT to that pickup branch and is assigned to that reservation, which stays WAITING |
| No WAITING reservation without an assigned copy; current branch is the home branch | Copy becomes AVAILABLE |
| No WAITING reservation without an assigned copy; current branch is not the home branch | Copy becomes IN_TRANSIT to the home branch |

On arrival, a librarian at the destination branch checks the copy in. A copy that is assigned to a reservation that is still WAITING becomes ON_HOLD for that reservation (N-06 is sent). In all other cases the table above is applied again. The current branch of the copy is updated on every check-in.

### 8.7 Overdue loans — F-LEN-07

- A loan is overdue from the first day after its due date.
- Overdue loans cannot be renewed (BR-LOA-05).
- Fines are described in §10, notices in §12.

---

## 9. Reservations (F-RES)

### 9.1 Placing a reservation — F-RES-01

A member can reserve a title in the portal; a librarian can do it at the desk on behalf of the member. The member chooses a pickup branch.

- **BR-RES-01** Reservations are placed on a title, not on a copy.
- A reservation is only possible when no copy of the title is AVAILABLE at the chosen pickup branch (otherwise the member can borrow it directly).
- **BR-RES-04** A member can have at most 5 active reservations (WAITING or ON_HOLD).
- **BR-RES-05** A member cannot reserve a title of which they currently have a copy on loan, and cannot have two active reservations for the same title.
- Blocked members and members with an expired membership cannot place reservations.

Reservation statuses: WAITING, ON_HOLD, COLLECTED, EXPIRED, CANCELLED.

### 9.2 Reservation fee — F-RES-02

- **BR-RES-03** The fee is **€0.50** for ADULT and SENIOR members and free for YOUTH members.
- The fee is charged when the reservation is placed and becomes a charge on the member's account.
- The fee is not refunded when the member cancels or does not collect the copy.
- When the library cancels the reservation (§7.3), the fee charge is cancelled; any amount already paid on it is refunded.

### 9.3 Queue — F-RES-03

- Reservations for a title are served first in, first out, by the time they were placed.
- When a copy becomes available (BR-RES-07), it is assigned to the first WAITING reservation that does not yet have a copy assigned, and routed according to §8.6.

### 9.4 Hold, reminder and expiry — F-RES-04

- When a copy becomes ON_HOLD, the reservation becomes ON_HOLD and the member receives N-06.
- **BR-RES-02** The copy is held for **7 days** after the pickup notification. **BR-RES-06** If the last hold day is a closing day, the hold is extended to the next opening day.
- A pickup reminder (N-07) is sent 5 days after the pickup notification.
- If the copy is not collected by the end of the last hold day, the reservation becomes EXPIRED in the daily processing of that day and N-08 is sent. The copy is assigned to the next WAITING reservation (§8.6) or becomes AVAILABLE.

### 9.5 Cancellation — F-RES-05

The member (portal) or a librarian (desk) can cancel a reservation that is WAITING or ON_HOLD. If a copy was on hold, it is assigned to the next WAITING reservation or becomes AVAILABLE. The fee is not refunded (BR-RES-03).

### 9.6 Effect on renewals

A WAITING reservation without an assigned copy prevents the renewal of all loans of that title (BR-LOA-05).

---

## 10. Fines, lost items and waivers (F-FIN)

### 10.1 Chargeable days — F-FIN-01

**BR-FIN-01** Overdue fines are calculated per copy, per chargeable day. Chargeable days are the **opening days** from the day after the due date up to and including the return date. Sundays and public holidays never count.

### 10.2 Daily rates — F-FIN-02

**BR-FIN-02** Daily rate per copy per chargeable day:

| Member category | BOOK | AUDIOVISUAL | MAGAZINE |
|---|---|---|---|
| ADULT | €0.25 | €1.00 | €0.10 |
| YOUTH | €0.00 | €0.50 | €0.00 |
| SENIOR | ADULT rate, 50% discount (§10.4) | ADULT rate, 50% discount | ADULT rate, 50% discount |

YOUTH members pay no overdue fines on BOOK and MAGAZINE.

**BR-FIN-11** The administrator maintains the rates with a valid-from date. The rates valid on the date of calculation (return date or lost declaration date) apply to all chargeable days of that loan. The member category used is the member's category on the date of calculation.

### 10.3 Grace period — F-FIN-03

**BR-FIN-03** Grace period of **1 opening day**:

- if the copy is returned on the first chargeable day, no fine is charged;
- if it is returned later, **all** chargeable days are charged, including the first one.

### 10.4 Senior discount — F-FIN-04

**BR-FIN-04** SENIOR members receive a 50% discount on the overdue fine.

### 10.5 Rounding and maximum — F-FIN-05

- **BR-FIN-05** The fine is rounded to whole cents using **half-up** rounding (€0.625 becomes €0.63). Rounding is done once per copy, after the discount and before the maximum.
- **BR-FIN-06** The overdue fine per copy is at most **€10.00 or the replacement cost of the copy, whichever is lower**.

### 10.6 Order of calculation — F-FIN-06

1. Count the chargeable days (BR-FIN-01).
2. If there is at most 1 chargeable day, the fine is €0.00 (BR-FIN-03). Stop.
3. Multiply the chargeable days by the daily rate (BR-FIN-02, BR-FIN-11).
4. For SENIOR members, apply the 50% discount (BR-FIN-04).
5. Round half-up to cents (BR-FIN-05).
6. Apply the maximum (BR-FIN-06).

Worked examples are in Appendix A.3. They are part of this specification.

### 10.7 Accrued fine — F-FIN-07

While an overdue loan is open, the member portal and the staff console show the fine that would be charged if the copy were returned on the current business date, together with the number of chargeable days. This amount is informative only: it is not a charge and does not count towards the account balance.

### 10.8 Lost declaration — F-FIN-08

- **BR-FIN-07** An open loan is declared lost in the daily processing of the date that is **42 calendar days after its due date**.
- On lost declaration:
  - the copy becomes LOST;
  - the loan is closed as lost; **BR-FIN-12** it no longer counts towards the member's loan limit;
  - the overdue fine is calculated with the declaration date as end date (§10.6, including the maximum) and charged;
  - the replacement cost of the copy (or the default) is charged;
  - an administration fee of **€5.00** is charged;
  - notification N-05 is sent.

### 10.9 Lost copy returned — F-FIN-09

- **BR-FIN-10** If a lost copy is returned within **90 days** of the lost declaration (the 90th day included), the replacement cost charge is cancelled; any amount already paid on it is refunded. The overdue fine and the administration fee remain due.
- **BR-FIN-13** If the copy is returned later, it is checked in, but nothing is cancelled or refunded.
- In both cases the copy is routed according to §8.6.

### 10.10 Waivers — F-FIN-10

- Staff can waive a charge fully or partially. A reason is always required.
- **BR-FIN-08** A librarian may waive at most **€2.00** per charge. **BR-FIN-14** This limit applies to the total amount waived by librarians on one charge, across all waivers. A branch manager may waive any amount.
- No staff member may waive a charge on their own member account.
- Administrators cannot waive charges.
- Every waiver is recorded with amount, reason, staff member, branch and date, and appears in report R-04.

---

## 11. Payments and account balance (F-PAY)

### 11.1 Desk payment — F-PAY-01

- A librarian registers payments in cash or by debit card.
- Partial payments are allowed. **BR-FIN-15** A desk payment cannot be higher than the account balance.
- **BR-FIN-09** A desk payment is allocated to the oldest outstanding charge first (by charge date), then the next oldest, and so on. A charge can be partially paid.
- The member receives a receipt (printed and/or e-mail N-10).

### 11.2 Online payment — F-PAY-02

- In the member portal the member chooses to pay the full balance or a selection of charges. Each selected charge is paid in full.
- The member is sent to the payment provider. In this implementation the payment provider is simulated (§15).
- If the payment succeeds, the payment is allocated to the selected charges (BR-FIN-09) and N-10 is sent. If it fails or is cancelled, nothing changes and the member sees a clear message.
- The same payment result must never be processed twice.

### 11.3 Charges and balance — F-PAY-03

- Charge types: OVERDUE_FINE, LOST_REPLACEMENT, LOST_ADMIN_FEE, RESERVATION_FEE.
- Each charge has an original amount, an outstanding amount, a charge date, a link to the loan or reservation it belongs to, and a status:
  - OPEN: outstanding amount above zero (possibly after partial payments or waivers);
  - SETTLED: outstanding amount zero, through payments and/or waivers;
  - CANCELLED: the charge no longer applies (§9.2, §10.9); any amount already paid on it is refunded to the member.
- The account balance is the sum of the outstanding amounts (OD-02).
- Members and staff can see every charge with its history (payments, waivers, refunds).

---

## 12. Notifications (F-NOT)

### 12.1 General rules — F-NOT-01

- Notifications are sent by e-mail. Members with SMS opt-in also receive N-01 and N-06 by SMS.
- Notifications for YOUTH members go to the guardian's e-mail address. SMS is not used for YOUTH members.
- Scheduled notifications are created by daily processing. N-06 and N-10 are sent immediately.
- Several scheduled notifications for the same recipient on the same day are combined into one message.
- E-mail notifications cannot be switched off.
- Every notification is logged with date, type, channel, recipient and delivery status.
- In this implementation, notifications are not really sent. They are written to a notification outbox that can be viewed in the staff console (§15).

### 12.2 Notification catalogue — F-NOT-02

| ID | Notification | Timing |
|---|---|---|
| N-01 | Due date reminder | 2 days before the due date |
| N-02 | First overdue notice | 1 day after the due date |
| N-03 | Second overdue notice | 7 days after the due date |
| N-04 | Third overdue notice | 21 days after the due date |
| N-05 | Lost declaration notice: copy declared lost, with the charges | 42 days after the due date (§10.8) |
| N-06 | Reservation ready for pickup, with branch and last pickup day | Immediately when the copy becomes ON_HOLD |
| N-07 | Pickup reminder | 5 days after N-06 |
| N-08 | Reservation expired | When the hold expires |
| N-09 | Membership expiry reminder | 30 days before the membership expires |
| N-10 | Payment receipt | Immediately after a payment |

All timings are in calendar days. N-01 to N-04 are only sent for loans that are still open on that day.

### 12.3 Content

Every notification contains the member's name, the titles concerned, the relevant dates and amounts (accrued fine or charges), and a link to the member portal.

---

## 13. Reports (F-REP)

| ID | Report | Content | Access |
|---|---|---|---|
| R-01 | Daily circulation | Checkouts, returns and renewals per branch and material type for a chosen day | Librarian (own branch), branch manager, administrator |
| R-02 | Overdue items | Open overdue loans with member, copy, due date and accrued fine | Librarian (own branch), branch manager, administrator |
| R-03 | Charges and payments summary | Per month: charges by type, payments by method, waivers, refunds, total outstanding balance | Branch manager, administrator |
| R-04 | Waivers | Every waiver with amount, reason, staff member, branch and date | Branch manager, administrator |
| R-05 | Lost items | Copies declared lost, replacement costs charged, refunded and paid | Branch manager, administrator |
| R-06 | Popular titles | Most borrowed titles per period and branch, based on all loans including anonymised ones | Branch manager, administrator |
| R-07 | Reservation queues | Titles with the most waiting reservations | Branch manager, administrator |

- Reports can be exported as CSV (F-REP-01) and PDF (F-REP-02).
- Reports never show the reading history of individual members, except R-02, which is needed to follow up overdue loans.
- Report R-03 can be sent monthly by e-mail to the Finance Controller as CSV (F-REP-03).

---

## 14. Administration (F-ADM)

| ID | Function |
|---|---|
| F-ADM-01 | Maintain the daily fine rates per member category and material type, each with a valid-from date. Past rates cannot be changed. |
| F-ADM-02 | Maintain public holidays (add, remove, view per year). |
| F-ADM-03 | Maintain the maximum number of simultaneous loans per member category. |
| F-ADM-04 | Manage staff accounts: create, deactivate, assign role and branch. |
| F-ADM-05 | View an audit trail of all changes to rates, holidays, loan limits and staff accounts, and of all financial operations (charges, payments, waivers, refunds), with date, user and old and new values. |

---

## 15. Training support (F-TRN)

These functions exist so that the system can be used for training and testing. They are only available in training mode, and only to the ADMINISTRATOR role. In production they are not available.

| ID | Function |
|---|---|
| F-TRN-01 | **Business date control.** Show the current business date everywhere in the staff console and the member portal. The administrator can set a new business date. Moving forward runs the missing daily processing runs in date order (§5.2). Moving backward is only allowed together with F-TRN-03. |
| F-TRN-02 | **Run daily processing now** for the current business date, and show a summary of what it did (notices created, copies declared lost, holds expired, loans anonymised). |
| F-TRN-03 | **Reset to seed data** (Appendix B), including the business date of the seed. |
| F-TRN-04 | **Notification outbox.** View all notifications with type, recipient, channel, date and content. |
| F-TRN-05 | **Simulated payment provider.** Online payments show a simple page where the user chooses "Payment succeeds", "Payment fails" or "Cancel". |
| F-TRN-06 | **Login as demo user.** A list of the seed accounts (Appendix B.1) to log in quickly with any role. |

---

## 16. Business rules index

| ID | Rule (short) | Section |
|---|---|---|
| BR-MEM-01 | Category by age at registration; re-evaluated at membership renewal | §6.1 |
| BR-MEM-02 | Maximum open loans: ADULT 10, YOUTH 5 (OD-01), SENIOR 10 | §6.4 |
| BR-MEM-03 | Blocked when balance ≥ €5.00 or membership expired | §6.5 |
| BR-MEM-04 | Membership fee: ADULT €12.00, YOUTH free, SENIOR €6.00 | §6.3 |
| BR-MEM-05 | Returned loans anonymised after 12 months, unless opt-in or outstanding charge | §6.7 |
| BR-MEM-06 | Category does not change between renewals | §6.1 |
| BR-MEM-07 | Start of a renewed membership period | §6.3 |
| BR-CAT-01 | Only AVAILABLE (or DAMAGED) copies can be set to DAMAGED or WITHDRAWN | §7.3 |
| BR-LOA-01 | Loan periods: BOOK 21, AUDIOVISUAL 7, MAGAZINE 14 days | §8.2 |
| BR-LOA-02 | Due date on a closing day moves to the next opening day | §8.3 |
| BR-LOA-03 | Maximum 2 renewals per loan | §8.4 |
| BR-LOA-04 | New due date = current due date + loan period (then BR-LOA-02) | §8.4 |
| BR-LOA-05 | Renewal refusal reasons | §8.4 |
| BR-LOA-06 | Return at any branch; book-drop return date | §8.5 |
| BR-LOA-07 | Checkout checks cannot be overridden | §8.1 |
| BR-RES-01 | Reservations are on titles | §9.1 |
| BR-RES-02 | Hold period 7 days | §9.4 |
| BR-RES-03 | Reservation fee €0.50 ADULT/SENIOR, free YOUTH; refunded only when the library cancels | §9.2 |
| BR-RES-04 | Maximum 5 active reservations | §9.1 |
| BR-RES-05 | No reservation for a title on loan to the member; no duplicate reservations | §9.1 |
| BR-RES-06 | Hold ending on a closing day is extended to the next opening day | §9.4 |
| BR-RES-07 | A copy that becomes available is first offered to the reservation queue | §7.3 |
| BR-FIN-01 | Fines per copy per chargeable (opening) day | §10.1 |
| BR-FIN-02 | Daily rates | §10.2 |
| BR-FIN-03 | Grace period of 1 opening day, retroactive when exceeded | §10.3 |
| BR-FIN-04 | SENIOR discount 50% | §10.4 |
| BR-FIN-05 | Half-up rounding once per copy, after discount, before maximum | §10.5 |
| BR-FIN-06 | Maximum per copy: lower of €10.00 and replacement cost | §10.5 |
| BR-FIN-07 | Lost declaration 42 calendar days after due date; replacement cost + €5.00 fee | §10.8 |
| BR-FIN-08 | Librarian waiver limit €2.00 per charge | §10.10 |
| BR-FIN-09 | Payment allocation: desk oldest first, online selected charges | §11 |
| BR-FIN-10 | Lost copy returned within 90 days: replacement cost cancelled (refunded if paid) | §10.9 |
| BR-FIN-11 | Rates valid on the calculation date apply | §10.2 |
| BR-FIN-12 | A lost loan no longer counts towards the loan limit | §10.8 |
| BR-FIN-13 | Lost copy returned after 90 days: no refund | §10.9 |
| BR-FIN-14 | Librarian waiver limit is cumulative per charge | §10.10 |
| BR-FIN-15 | A desk payment cannot exceed the account balance | §11.1 |

---

## 17. Feature list and increments

| Feature | Description | Priority | Increment |
|---|---|---|---|
| §3 | Login, roles and permission checks | Must | 1 |
| F-MEM-01 | Member categories | Must | 1 |
| F-MEM-02 | Register a member at the desk | Must | 1 |
| F-MEM-04 | Membership validity and fees (desk) | Must | 1 |
| F-MEM-05 | Borrowing entitlements | Must | 1 |
| F-MEM-06 | Account balance and blocking | Must | 1 |
| F-CAT-01 | Titles and copies | Must | 1 |
| F-CAT-02 | Material types | Must | 1 |
| F-CAT-03 | Copy statuses | Must | 1 |
| F-CAT-04 | Catalogue search and availability | Must | 1 |
| F-LEN-01 | Checkout | Must | 1 |
| F-LEN-02 | Loan periods | Must | 1 |
| F-LEN-03 | Due date | Must | 1 |
| F-LEN-04 | Renewal (desk) | Must | 1 |
| F-LEN-05 | Return (without book drop) | Must | 1 |
| F-LEN-06 | Routing of returned copies (reservation rows apply from increment 2) | Must | 1 |
| F-LEN-07 | Overdue loans | Must | 1 |
| F-FIN-01 to F-FIN-07 | Fine calculation and accrued fine | Must | 1 |
| F-PAY-01 | Desk payment | Must | 1 |
| F-PAY-03 | Charges and balance | Must | 1 |
| F-ADM-01 | Maintain fine rates | Must | 1 |
| F-ADM-02 | Maintain public holidays | Must | 1 |
| F-TRN-01, 02, 03, 06 | Business date, daily processing, reset, demo login | Must | 1 |
| F-MEM-07 | Self-service in the member portal (incl. online loan renewal) | Should | 2 |
| F-MEM-08 | Privacy and anonymisation | Should | 2 |
| F-RES-01 to F-RES-05 | Reservations, fee, queue, holds, cancellation | Should | 2 |
| F-FIN-08 | Lost declaration | Should | 2 |
| F-FIN-09 | Lost copy returned | Should | 2 |
| F-FIN-10 | Waivers | Should | 2 |
| F-PAY-02 | Online payment (simulated) + F-TRN-05 | Should | 2 |
| F-NOT-01, F-NOT-02 | Notifications (e-mail via outbox) + F-TRN-04 | Should | 2 |
| R-01, R-02, F-REP-01 | Circulation and overdue reports, CSV export | Should | 2 |
| F-MEM-03 | Online pre-registration | Could | 3 |
| F-MEM-04 | Online membership renewal with payment | Could | 3 |
| F-MEM-09 | Closing a membership | Could | 3 |
| F-LEN-05 | Book-drop returns | Could | 3 |
| F-NOT-01 | SMS channel | Could | 3 |
| R-03 to R-07, F-REP-02, F-REP-03 | Management reports, PDF export, monthly e-mail | Could | 3 |
| F-ADM-03 | Maintain loan limits | Could | 3 |
| F-ADM-04 | Manage staff accounts | Could | 3 |
| F-ADM-05 | Audit trail | Could | 3 |

---

## 18. Open decisions

| ID | Question | Provisional decision | Owner |
|---|---|---|---|
| OD-01 | What is the maximum number of simultaneous loans for YOUTH members? | **5**. Implement so that the value can be changed without rework (F-ADM-03). | Product owner |
| OD-02 | Do reservation fees count towards the account balance and therefore towards the blocking threshold? | **Yes**: reservation fees are ordinary charges. | Product owner |

---

## 19. Clarifications added in this specification

The following rules are not described (or not described unambiguously) in the analysis pack. They were added to make this specification implementable. They should be confirmed by the product owner.

| Item | Section |
|---|---|
| Category does not change between membership renewals (BR-MEM-06) | §6.1 |
| Start date of a renewed membership period (BR-MEM-07); online membership renewal with online payment | §6.3 |
| Guardian can be an ADULT or SENIOR member | §6.2 |
| Closing a membership removes contact details from retained financial records | §6.8 |
| Status rules for damaged and withdrawn copies (BR-CAT-01); withdrawing the last copy cancels its reservations | §7.3 |
| Checkout checks cannot be overridden (BR-LOA-07) | §8.1 |
| Routing table, including copies in transit to a reservation | §8.6 |
| No duplicate reservations and no reservation for a title on loan to the member (BR-RES-05) | §9.1 |
| Hold extended when the last hold day is a closing day (BR-RES-06) | §9.4 |
| Any copy that becomes available is first offered to the queue (BR-RES-07) | §7.3, §9.3 |
| Rates valid on the calculation date apply (BR-FIN-11) | §10.2 |
| A lost loan no longer counts towards the loan limit (BR-FIN-12) | §10.8 |
| No refund for lost copies returned after 90 days (BR-FIN-13) | §10.9 |
| Librarian waiver limit is cumulative per charge (BR-FIN-14) | §10.10 |
| Desk payment cannot exceed the balance; online payments pay selected charges in full (BR-FIN-15) | §11 |
| Charge type RESERVATION_FEE; charge statuses OPEN, SETTLED, CANCELLED | §11.3 |
| SMS is not used for YOUTH members | §12.1 |
| Daily processing semantics: every calendar day, after closing, with catch-up runs | §5.2 |
| Loan limits configurable by the administrator | §4, §14 |
| Training support functions, simulated payments and notification outbox | §15 |
| Seed data instead of migration | Appendix B |

---

## Appendix A — Acceptance examples

These examples are part of the specification. Every example must be covered by an automated test that references its ID. Amounts are in euro. Weekdays are given to make the calendar checks easy. Unless stated otherwise, copies have the default replacement cost.

### A.1 Due dates and renewals

| ID | Situation | Expected result |
|---|---|---|
| AC-LEN-01 | BOOK checked out on Mon 2026-03-02 | Due date Mon 2026-03-23 |
| AC-LEN-02 | BOOK checked out on Mon 2026-03-16 | 2026-04-06 is Easter Monday, so the due date is Tue 2026-04-07 |
| AC-LEN-03 | AUDIOVISUAL checked out on Thu 2026-05-07 | 2026-05-14 is Ascension Day, so the due date is Fri 2026-05-15 |
| AC-LEN-04 | BOOK due Mon 2026-03-23, renewed on Tue 2026-03-10 | New due date Mon 2026-04-13 (current due date + 21 days, not renewal date + 21) |
| AC-LEN-05 | The same loan renewed again on Wed 2026-04-01 | New due date Mon 2026-05-04 |
| AC-LEN-06 | The same loan, third renewal attempt | Refused: maximum number of renewals reached |
| AC-LEN-07 | BOOK due Mon 2026-03-16, renewed on Thu 2026-03-12 | 2026-04-06 is Easter Monday, so the new due date is Tue 2026-04-07 |
| AC-LEN-08 | Renewal while another member has a WAITING reservation (no copy assigned) on the title | Refused: reservations waiting |
| AC-LEN-09 | Renewal of an overdue loan | Refused: loan overdue |
| AC-LEN-10 | Renewal by a member with balance €5.00 | Refused: member blocked |
| AC-LEN-11 | BOOK due Sat 2026-03-14, dropped in the book drop, checked in on Mon 2026-03-16 as book-drop return | Return date Sat 2026-03-14; no fine |
| AC-LEN-12 | BOOK with home branch CEN returned at RIV, no reservations | Copy IN_TRANSIT to CEN; after check-in at CEN: AVAILABLE |
| AC-LEN-13 | BOOK returned at CEN; the first WAITING reservation has pickup branch CEN | Copy ON_HOLD for that reservation; N-06 sent immediately |

### A.2 Members, limits and blocking

| ID | Situation | Expected result |
|---|---|---|
| AC-MEM-01 | ADULT with balance €4.99 checks out a copy | Allowed |
| AC-MEM-02 | ADULT with balance €5.00 tries to check out, renew and reserve | All three refused: member blocked |
| AC-MEM-03 | The member of AC-MEM-02 pays €0.01 | Balance €4.99; checkout allowed immediately |
| AC-MEM-04 | Membership expired yesterday; member returns a copy and pays a charge | Return and payment allowed; checkout, renewal and reservation refused |
| AC-MEM-05 | ADULT with 10 open loans tries an 11th checkout | Refused: loan limit reached |
| AC-MEM-06 | YOUTH with 5 open loans tries a 6th checkout | Refused: loan limit reached (OD-01) |
| AC-MEM-07 | Person born 2008-06-15 registers on 2026-03-02 | Category YOUTH. On 2026-06-15 still YOUTH. After membership renewal on 2027-03-02: ADULT |
| AC-MEM-08 | Loan returned on Mon 2025-03-17, member without opt-in, no outstanding charge on the loan | Anonymised in the daily processing of 2026-03-17 |
| AC-MEM-09 | As AC-MEM-08, but the member has opted in to keep the reading history | Not anonymised |
| AC-MEM-10 | As AC-MEM-08, but the loan has an overdue fine with €1.00 outstanding | Not anonymised; anonymised in the first daily processing after the charge is settled |

### A.3 Overdue fines

| ID | Member | Material | Due date | Returned | Chargeable days | Calculation | Fine |
|---|---|---|---|---|---|---|---|
| AC-FIN-01 | ADULT | BOOK | Mon 2026-03-02 | Tue 2026-03-03 | 1 | Within grace period | €0.00 |
| AC-FIN-02 | ADULT | BOOK | Mon 2026-03-02 | Wed 2026-03-04 | 2 | 2 × €0.25 | €0.50 |
| AC-FIN-03 | ADULT | BOOK | Fri 2026-03-06 | Fri 2026-03-13 | 6 | Sunday 2026-03-08 not counted; 6 × €0.25 | €1.50 |
| AC-FIN-04 | ADULT | BOOK | Sat 2026-04-04 | Wed 2026-04-08 | 2 | Sunday 5 April and Easter Monday 6 April not counted; 2 × €0.25 | €0.50 |
| AC-FIN-05 | SENIOR | BOOK | Mon 2026-03-02 | Sat 2026-03-07 | 5 | 5 × €0.25 = €1.25; −50% = €0.625; half-up | €0.63 |
| AC-FIN-06 | ADULT | AUDIOVISUAL, replacement cost €8.00 | Mon 2026-03-02 | Mon 2026-03-16 | 12 | 12 × €1.00 = €12.00; maximum = lower of €10.00 and €8.00 | €8.00 |
| AC-FIN-07 | ADULT | AUDIOVISUAL, default replacement cost €20.00 | Mon 2026-03-02 | Mon 2026-03-16 | 12 | €12.00; maximum €10.00 | €10.00 |
| AC-FIN-08 | SENIOR | AUDIOVISUAL | Mon 2026-03-02 | Thu 2026-03-05 | 3 | 3 × €1.00 = €3.00; −50% | €1.50 |
| AC-FIN-09 | SENIOR | AUDIOVISUAL | Mon 2026-03-02 | Tue 2026-03-31 | 25 | €25.00; −50% = €12.50; maximum €10.00 | €10.00 |
| AC-FIN-10 | YOUTH | BOOK | Mon 2026-03-02 | Mon 2026-03-16 | 12 | Rate €0.00 | €0.00 |
| AC-FIN-11 | YOUTH | AUDIOVISUAL | Mon 2026-03-02 | Wed 2026-03-04 | 2 | 2 × €0.50 | €1.00 |
| AC-FIN-12 | ADULT | MAGAZINE | Mon 2026-03-02 | Sat 2026-03-07 | 5 | 5 × €0.10 | €0.50 |
| AC-FIN-13 | ADULT | BOOK | Mon 2026-03-02 | Not returned; business date Wed 2026-03-04 | 2 | Accrued fine shown in portal and staff console; no charge created; balance unchanged | €0.50 (accrued) |

### A.4 Lost copies

| ID | Situation | Expected result |
|---|---|---|
| AC-FIN-14 | ADULT, BOOK (default replacement cost €25.00) due Mon 2026-03-02, not returned. Daily processing of Sun 2026-04-12 | Not lost yet (41 days) |
| AC-FIN-15 | Same loan, daily processing of Mon 2026-04-13 (42 days) | Copy LOST; loan closed as lost and no longer counts towards the limit; charges: overdue fine €8.75 (35 chargeable days × €0.25), replacement cost €25.00, administration fee €5.00 — total €38.75; N-05 sent |
| AC-FIN-16 | The lost copy is returned on 2026-05-20 | Replacement cost charge of €25.00 CANCELLED (any amount paid on it refunded); €8.75 and €5.00 remain due |
| AC-FIN-17 | The lost copy is returned on 2026-07-13 (91 days after the declaration) | Copy checked in; nothing cancelled or refunded. A return on 2026-07-12 would still cancel the replacement cost charge |

### A.5 Waivers

| ID | Situation | Expected result |
|---|---|---|
| AC-FIN-18 | Librarian waives €2.00 of a €3.00 charge, with a reason | Allowed; outstanding €1.00 |
| AC-FIN-19 | A librarian then waives another €0.50 on the same charge | Refused: librarian limit of €2.00 per charge exceeded |
| AC-FIN-20 | Librarian waives €2.01 on a new charge | Refused. A branch manager can waive the same amount |
| AC-FIN-21 | Waiver without a reason | Refused |
| AC-FIN-22 | A librarian or branch manager waives a charge on their own member account | Refused |
| AC-FIN-23 | Administrator tries to waive a charge | Not possible |

### A.6 Payments

| ID | Situation | Expected result |
|---|---|---|
| AC-PAY-01 | Member has charges A €1.00 (2026-03-01), B €3.00 (2026-03-10) and C €2.00 (2026-03-14). Desk payment €3.50 | A SETTLED; B OPEN with €0.50 outstanding; C unchanged; balance €2.50; member no longer blocked |
| AC-PAY-02 | Balance €5.00, desk payment of €6.00 | Refused: payment exceeds balance |
| AC-PAY-03 | Online payment of charge C only, payment succeeds | C paid; A and B unchanged; N-10 sent |
| AC-PAY-04 | Online payment fails or is cancelled | Nothing changes; member sees a clear message; no N-10 |
| AC-PAY-05 | The payment provider reports the same successful payment twice | Processed once |

### A.7 Reservations

| ID | Situation | Expected result |
|---|---|---|
| AC-RES-01 | ADULT places a reservation | Charge of €0.50 (RESERVATION_FEE) |
| AC-RES-02 | YOUTH places a reservation | No charge |
| AC-RES-03 | Member with 5 active reservations places a 6th | Refused |
| AC-RES-04 | Member reserves a title with an AVAILABLE copy at the chosen pickup branch | Refused: the copy can be borrowed directly |
| AC-RES-05 | Member reserves a title of which they have a copy on loan | Refused |
| AC-RES-06 | Members X then Y reserve the same title (pickup CEN). A copy is returned at CEN | ON_HOLD for X; N-06 to X |
| AC-RES-07 | N-06 sent on Mon 2026-03-02, copy not collected | N-07 on Sat 2026-03-07; last hold day Mon 2026-03-09; reservation EXPIRED in the daily processing of 2026-03-09; N-08 to X; copy ON_HOLD for Y and N-06 to Y |
| AC-RES-08 | N-06 sent on Thu 2026-05-07 | Day 7 is Thu 2026-05-14 (Ascension Day), so the last hold day is Fri 2026-05-15; N-07 on Tue 2026-05-12 |
| AC-RES-09 | Member cancels a reservation | Reservation CANCELLED; reservation fee not refunded |
| AC-RES-10 | The library withdraws the last copy of a title with two WAITING reservations | Both reservations CANCELLED by the library; both fee charges CANCELLED (refund of any amount paid) |

### A.8 Notifications

| ID | Situation | Expected result |
|---|---|---|
| AC-NOT-01 | Loan due Mon 2026-03-23, never returned | N-01 on Sat 2026-03-21; N-02 on Tue 2026-03-24; N-03 on Mon 2026-03-30; N-04 on Mon 2026-04-13; lost declaration and N-05 on Mon 2026-05-04 |
| AC-NOT-02 | The same loan is returned on 2026-03-25 | N-01 and N-02 were sent; N-03 and later are not sent |
| AC-NOT-03 | Notifications for a YOUTH member | Sent to the guardian's e-mail address; never by SMS |
| AC-NOT-04 | Member with SMS opt-in receives N-01 and N-02 | N-01 by e-mail and SMS; N-02 by e-mail only |
| AC-NOT-05 | Member has N-01 for two loans on the same day | One combined message |

### A.9 Permissions

| ID | Situation | Expected result |
|---|---|---|
| AC-ROL-01 | Librarian of RIV requests R-02 for CEN | Refused |
| AC-ROL-02 | Administrator tries to check out a copy | Not possible |
| AC-ROL-03 | Member requests another member's loans | Refused |
| AC-ROL-04 | Librarian requests R-03 | Refused |
| AC-ROL-05 | Anonymous visitor searches the catalogue | Allowed, with availability per branch |

---

## Appendix B — Seed data

The seed represents the situation at the **start of business date Monday 2026-03-16**. The daily processing of 2026-03-16 has not run yet. All names are fictional. The notification outbox starts empty.

### B.1 Staff accounts

| Account | Role | Branch | Notes |
|---|---|---|---|
| admin | ADMINISTRATOR | — | |
| manager.cen | BRANCH_MANAGER | Central | |
| librarian.cen | LIBRARIAN | Central | Same person as member Sam Stone (S-19) |
| librarian.riv | LIBRARIAN | Riverside | |

### B.2 Catalogue

- The six branches of §1.1.
- At least 30 titles: about 20 BOOK, 6 AUDIOVISUAL and 4 MAGAZINE, with copies spread over all branches (at least 80 copies in total).
- One BOOK title with exactly **one copy** (home branch Central), called *The Lighthouse Keeper's Atlas*.
- One AUDIOVISUAL copy with a replacement cost of **€8.00**.
- The 2026 public holidays of §5.1 and the rates of §10.2 (valid from 2026-01-01).

### B.3 Members and scenarios

| ID | Member | Category | Situation in the seed | What it is for |
|---|---|---|---|---|
| S-01 | Ada Adams | ADULT | No loans, no charges | Happy path |
| S-02 | Ben Brooks | ADULT | 10 open BOOK loans, due Mon 2026-03-30 | Loan limit (AC-MEM-05) |
| S-03 | Cleo Carter | YOUTH, guardian S-04 | 5 open BOOK loans, not overdue | YOUTH loan limit (AC-MEM-06), guardian notifications |
| S-04 | Dan Carter | ADULT | Guardian of S-03 and S-13 | Guardian |
| S-05 | Eva Evans | ADULT | One OVERDUE_FINE charge of €5.00 outstanding | Blocked at exactly €5.00 (AC-MEM-02/03) |
| S-06 | Finn Foster | ADULT | One OVERDUE_FINE charge of €5.25, of which €0.26 paid: €4.99 outstanding | Not blocked at €4.99 (AC-MEM-01) |
| S-07 | Gina Gray | ADULT | Membership valid until 2026-03-10 | Expired membership (AC-MEM-04) |
| S-08 | Hugo Hale | SENIOR | BOOK loan due Mon 2026-03-02, not returned | Accrued fine €1.50 on 2026-03-16 (12 days × €0.25, −50%) |
| S-09 | Iris Irwin | ADULT | BOOK loan (default replacement cost) due Tue 2026-02-03, not returned | Declared lost in the daily processing of 2026-03-17: fine €9.00 (36 days), replacement €25.00, fee €5.00 |
| S-10 | Jack Jones | ADULT | BOOK loan checked out 2026-01-19, already renewed twice, due Mon 2026-03-23 | Maximum renewals |
| S-11 | Kim King | ADULT | Has the only copy of *The Lighthouse Keeper's Atlas*, checked out Mon 2026-03-09, due Mon 2026-03-30, not renewed | Renewal refused because of S-12's reservation (AC-LEN-08) |
| S-12 | Leo Lane | ADULT | 5 WAITING reservations, pickup Central, including *The Lighthouse Keeper's Atlas*; the other 4 are titles whose copies are all on loan. 5 RESERVATION_FEE charges of €0.50 outstanding (balance €2.50) | Reservation limit (AC-RES-03), queue, OD-02 |
| S-13 | Mia Moore | YOUTH, guardian S-04 | BOOK loan and AUDIOVISUAL loan, both due Mon 2026-03-02, not returned | Accrued fines on 2026-03-16: BOOK €0.00, AUDIOVISUAL €6.00 |
| S-14 | Noah Nash | ADULT | "Keep my reading history" on; loans returned on 2025-01-15 and 2025-02-10 | Not anonymised (AC-MEM-09); withdraw opt-in to test |
| S-15 | Olga Owen | ADULT | No opt-in. Loan returned Mon 2025-03-17, no charges. Loan returned 2025-02-03 with an OVERDUE_FINE of €1.00 outstanding | Anonymisation in the daily processing of 2026-03-17 (AC-MEM-08); second loan stays linked (AC-MEM-10) |
| S-16 | Pia Price | ADULT | Reservation ON_HOLD at Central since Mon 2026-03-09 (N-06 sent that day); last hold day 2026-03-16 | Hold expiry in the daily processing of 2026-03-16 |
| S-17 | Quinn Quill | ADULT | WAITING reservation, pickup Central, second in the queue for the same title as S-16 | Copy goes on hold for Quinn after Pia's hold expires |
| S-18 | Rosa Reed | ADULT | Charges €1.00 (2026-03-01), €3.00 (2026-03-10) and €2.00 (2026-03-14), all outstanding; balance €6.00 | Payment allocation (AC-PAY-01) |
| S-19 | Sam Stone | ADULT | One OVERDUE_FINE charge of €1.50 outstanding; same person as staff account librarian.cen | Own-account waiver rule (AC-FIN-22) |

---

## Appendix C — Training exercises

**Not part of the implementation scope.** These exercises are for participants who use an AI agent to review, test and change the implemented application. Each exercise has a goal, a suggested prompt and what to look for in the result. The suggested prompts are starting points; participants are encouraged to improve them.

### C.1 Coverage and traceability

**EX-01 — Feature coverage audit**
- *Goal:* determine which features of §17 are implemented, partially implemented or missing.
- *Prompt:* "Using §17 of the specification, check for every feature whether it is implemented. For each, give the status (implemented / partial / missing) and the evidence: where in the application it lives and how you verified the behaviour. Do not rely on names of files or screens alone."
- *Look for:* evidence based on behaviour rather than on names; a clear explanation of what "partial" means for each feature; honest "could not verify" answers.

**EX-02 — Business rule audit**
- *Goal:* check that every rule in §16 is enforced, and enforced everywhere it applies.
- *Prompt:* "For every business rule in §16, show where it is enforced and which test proves it. List rules that are not enforced, enforced only in one channel (portal or desk), or enforced without a test."
- *Look for:* rules enforced in the portal but not at the desk (or the other way around), for example renewal rules; rules that are only checked by hiding a button.

**EX-03 — Acceptance examples as tests**
- *Goal:* make sure every example in Appendix A is covered by a test that passes.
- *Prompt:* "Check that every acceptance example in Appendix A has an automated test that references its ID and really tests what the example describes. Add missing tests, run all of them and report which pass and which fail. Do not change expected values; if you think an example is wrong, explain why."
- *Look for:* complete coverage of AC-IDs; whether the agent tries to "fix" a failing test by changing the expected value instead of the code; whether failures are explained in business terms.

**EX-04 — Permission matrix**
- *Goal:* verify every cell of the matrix in §3.2.
- *Prompt:* "Verify every cell of the permission matrix in §3.2 by actually trying each function with each role, using the demo accounts. Report every deviation."
- *Look for:* the €2.00 librarian limit (cumulative, BR-FIN-14), the own-account rule, branch scoping of R-01 and R-02.

### C.2 Scenarios and boundaries

**EX-05 — Walk a loan through time**
- *Goal:* use the business date to follow one loan from checkout to lost declaration.
- *Prompt:* "Check out a BOOK for Ada Adams on the seed date. Move the business date forward step by step and verify every notification, accrued fine and status change against the specification until the copy is declared lost. Then return the copy and verify the refund rules."
- *Look for:* correct dates of N-01 to N-05, the accrued fine per day, Sundays and holidays, catch-up runs when moving several days at once.

**EX-06 — Boundary values**
- *Goal:* test both sides of every numeric boundary.
- *Prompt:* "List every numeric boundary in the specification (amounts, counts, days). For each, define a test just below, at and just above the boundary, and run them."
- *Look for:* €5.00 blocking, loan and reservation limits, 2 renewals, grace day, 42 days, 90 days, €2.00 waiver limit, €10.00 maximum, 7-day hold.

**EX-07 — Calendar edge cases**
- *Goal:* verify everything that depends on closing days.
- *Prompt:* "Find every rule that depends on the calendar. Design test cases around Easter Monday, Ascension Day and Sundays, including a book-drop return over a weekend and a hold ending on a holiday."
- *Look for:* due date shifts, hold extensions, chargeable days, book-drop return dates.

### C.3 Change requests

For each change request, first ask the agent for an **impact analysis** (which features, rules, acceptance examples, notifications, reports and seed data are affected, and which questions are open), review it, and only then let it implement.

**CR-01 — Three renewals for books.** The maximum number of renewals for BOOK becomes 3. AUDIOVISUAL and MAGAZINE stay at 2.
*Look for:* AC-LEN-06 needs to change; the portal shows the number of renewals left per loan.

**CR-02 — New material type BOARD_GAME.** Loan period 14 days, daily rate €0.50 (ADULT and SENIOR rate before discount), €0.25 for YOUTH, default replacement cost €40.00, at most 2 board games on loan per member.
*Look for:* completeness (search, statuses, fines, maximum per copy, reports, seed data, acceptance examples) and the open question of whether the 2-game limit counts towards the general loan limit.

**CR-03 — Fine amnesty.** The administrator can define an amnesty period. Copies returned during that period are charged no overdue fine.
*Look for:* questions before implementation — does it apply to lost copies returned during the amnesty, to charges created before the period, to the accrued fine shown in the portal?

**CR-04 — Branch closed for renovation.** Harbour is closed for two weeks.
*Look for:* the agent should notice that the specification has one library-wide calendar (§5.1) and ask whose calendar applies to due dates, fines and holds — the checkout branch, the home branch or the pickup branch — rather than choosing silently.

**CR-05 — Youth fine-free for audiovisual too.**
*Look for:* recognising that this may be a rate change rather than a code change (F-ADM-01), and that AC-FIN-11, the seed scenario S-13 and member communication must be updated.

### C.4 Specification quality

**EX-08 — Ambiguity hunt**
- *Goal:* find statements in this specification that can be implemented in more than one way.
- *Prompt:* "List every statement in this specification that two developers could reasonably implement differently. For each, give both interpretations and the question you would ask the product owner."
- *Look for:* compare the lists of different groups; check whether the implementation chose one interpretation silently.

**EX-09 — Specification versus analysis pack**
- *Goal:* explain every difference between this specification and the original analysis documents.
- *Prompt:* "Compare this specification with the analysis pack. List every difference. For each, find the decision (meeting minutes, ADR, newer document) that justifies the specification, or state that you found none."
- *Trainer note:* do this exercise **after** the exercises on the analysis pack itself, because this specification contains the resolved values. The clarifications in §19 have no source in the analysis pack; a good result recognises them as additions.

**EX-10 — Assumptions made during implementation**
- *Goal:* make implicit decisions explicit.
- *Prompt:* "List every decision you or another agent made during implementation that is not explicitly specified. How were OD-01 and OD-02 implemented, and how easy are they to change?"
- *Look for:* undocumented assumptions; whether the YOUTH loan limit can be changed without code changes.

### C.5 Review and quality

**EX-11 — Bug hunt**
- *Goal:* find bugs that the trainer has deliberately introduced, without looking at the change history.
- *Prompt:* "The fine calculation and due date logic may contain bugs. Find them by reasoning from the specification and by running targeted examples. For each bug, give a reproducing example before proposing a fix."
- *Look for:* reproduction before fixing; fixes that also add a test with an AC-style ID.

**EX-12 — Explain a fine to a member**
- *Goal:* check that the calculation can be explained.
- *Prompt:* "Pick three charges from the seed data after advancing the business date by two weeks. Explain each to the member in plain language, with a day-by-day breakdown, and check the result against §10."
- *Look for:* correct handling of Sundays, holidays, grace, senior discount, rounding and the maximum; a tone suitable for members.

**EX-13 — Desk guide**
- *Goal:* generate end-user documentation and verify it.
- *Prompt:* "Write a one-page quick reference for librarians at the desk based on the application. Then verify every statement in it against the specification and the actual behaviour."
- *Look for:* statements that sound plausible but are not true for this application.

**EX-14 — Regression after a change**
- *Goal:* check that a change request did not break anything else.
- *Prompt:* "After implementing CR-01 or CR-02, run all acceptance tests. List which acceptance examples had to be changed and why, and confirm that no other behaviour changed."
- *Look for:* examples changed without a reason in the change request.