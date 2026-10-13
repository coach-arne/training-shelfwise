# Business Rules Catalogue

| Field | Value |
|---|---|
| Document ID | BR-catalogue |
| Version | 1.0 |
| Status | Approved |
| Owner | Tom Hollis, Business Analyst |
| Last updated | 2026-04-14 |

This catalogue consolidates the business rules from the functional analysis. The source column refers to the document where the rule is described in detail. If this catalogue and the source document disagree, the source document prevails.

## Members

| ID | Rule | Source |
|---|---|---|
| BR-MEM-01 | Member category by age: YOUTH 0–17, ADULT 18–64, SENIOR 65+. Re-evaluated at every membership renewal. | FA-02 §1 |
| BR-MEM-02 | Maximum simultaneous loans: ADULT 10, YOUTH 5, SENIOR 10. | FA-02 §4 |
| BR-MEM-03 | A member is blocked when the outstanding balance is €5.00 or more, or the membership has expired. Blocked members cannot borrow, renew or reserve. | FA-02 §5 |
| BR-MEM-04 | Annual membership fee: ADULT €12.00, YOUTH free, SENIOR €6.00. | FA-02 §3 |
| BR-MEM-05 | Loans are anonymised 12 months after return, unless the member opted in to keep the reading history. Loans with unpaid charges are not anonymised. | FA-02 §7 |

## Loans

| ID | Rule | Source |
|---|---|---|
| BR-LOA-01 | Loan periods: BOOK 21 days, AUDIOVISUAL 7 days, MAGAZINE 14 days. | FA-04 §2 |
| BR-LOA-02 | A due date that falls on a closing day moves to the next opening day. | FA-04 §3 |
| BR-LOA-03 | A loan can be renewed at most 2 times. | FA-04 §4 |
| BR-LOA-04 | New due date after renewal = current due date + loan period, then BR-LOA-02. | FA-04 §4 |
| BR-LOA-05 | A renewal is refused when the title has waiting reservations, the member is blocked or the membership has expired, the loan is overdue, or the maximum number of renewals is reached. | FA-04 §4 |
| BR-LOA-06 | Copies can be returned at any branch. The return date is the check-in date; book-drop returns get the date of the previous opening day. | FA-04 §5 |

## Reservations

| ID | Rule | Source |
|---|---|---|
| BR-RES-01 | Reservations are placed on a title and processed first in, first out. | FA-05 §1, §3 |
| BR-RES-02 | A copy stays on hold for 7 days after the member is notified. | FA-05 §4 |
| BR-RES-03 | Reservation fee: €0.50 for ADULT and SENIOR, free for YOUTH. Charged when the reservation is placed; refunded only when the library cancels. | FA-05 §2 |
| BR-RES-04 | A member can have at most 5 active reservations. | FA-05 §1 |

## Fines and payments

| ID | Rule | Source |
|---|---|---|
| BR-FIN-01 | Overdue fines are charged per copy per chargeable (opening) day. | FA-06 §3.1 |
| BR-FIN-02 | Daily rates — ADULT: BOOK €0.25, AUDIOVISUAL €1.00, MAGAZINE €0.10. YOUTH: BOOK €0.10, AUDIOVISUAL €0.50, MAGAZINE €0.05. SENIOR: ADULT rates with discount (BR-FIN-04). | FA-06 §3.2 |
| BR-FIN-03 | Grace period of 1 opening day. If the copy is returned after the grace period, all chargeable days are charged, including the first. | FA-06 §3.3 |
| BR-FIN-04 | SENIOR members receive a 50% discount on the overdue fine. | FA-06 §3.4 |
| BR-FIN-05 | Half-up rounding to two decimals, once per copy, after the discount and before the maximum. | FA-06 §3.5 |
| BR-FIN-06 | Maximum overdue fine per copy: €10.00 or the replacement cost, whichever is lower. | FA-06 §3.6 |
| BR-FIN-07 | A copy is declared lost 42 days after its due date. The member is charged the replacement cost and a €5.00 administration fee. | FA-06 §4 |
| BR-FIN-08 | Waivers: librarians up to €2.00 per charge, Branch Managers without limit; a reason is mandatory. | Fines workshop 2026-04-02, D9 |
| BR-FIN-09 | Desk payments are allocated to the oldest outstanding charge first; online payments to the charges selected by the member. | FA-06 §6.3 |
| BR-FIN-10 | If a lost copy is returned within 90 days of the lost declaration, the replacement cost is refunded; the overdue fine and administration fee remain due. | FA-06 §4.3 |
