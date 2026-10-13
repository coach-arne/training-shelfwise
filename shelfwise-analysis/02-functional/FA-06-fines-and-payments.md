# FA-06 — Fines and Payments

| Field | Value |
|---|---|
| Document ID | FA-06 |
| Version | 2.0 |
| Status | Approved |
| Owner | Tom Hollis, Business Analyst |
| Last updated | 2026-04-10 |
| Related | FA-02, FA-04, FA-09, BR-catalogue, minutes 2026-04-02 |

## 1. Purpose

This document describes overdue fines, lost items, waivers and payments. Version 2.0 incorporates the decisions of the fines workshop of 2 April 2026.

## 2. Principles

- Fines must be predictable: a member can always see in the portal what a loan will cost if returned today.
- Fines are calculated per loan (per copy), never per member or per visit.
- The library charges only for days on which it was open.

## 3. Overdue fines

### 3.1 Chargeable days

Only opening days count (BR-FIN-01). Closing days — Sundays and public holidays as listed in `06-reference/public-holidays-2026.csv` — are not counted. The count starts on the day after the due date and ends on the return date (inclusive).

### 3.2 Daily rates

Daily rate per copy and per chargeable day (BR-FIN-02):

| Member category | BOOK | AUDIOVISUAL | MAGAZINE |
|---|---|---|---|
| ADULT | €0.25 | €1.00 | €0.10 |
| YOUTH | €0.10 | €0.50 | €0.05 |
| SENIOR | Adult rate with discount (§3.4) | Adult rate with discount (§3.4) | Adult rate with discount (§3.4) |

The authoritative tariff table is maintained by Finance in `06-reference/fine-tariffs.csv`. In case of doubt, the CSV prevails over this table.

### 3.3 Grace period

There is a grace period of **1 opening day** (BR-FIN-03):

- If the copy is returned on the first chargeable day after the due date, no fine is charged.
- If the copy is returned later, **all** chargeable days are charged, including the first one.

### 3.4 Senior discount

SENIOR members receive a **50% discount** on the calculated overdue fine (BR-FIN-04).

### 3.5 Rounding

Amounts are rounded to two decimals using **half-up** rounding (0.125 becomes 0.13). Rounding is applied once per copy, after the discount and before the maximum (BR-FIN-05).

### 3.6 Maximum per copy

The overdue fine per copy is capped at **€10.00 or the replacement cost of the copy, whichever is lower** (BR-FIN-06).

### 3.7 Order of calculation

1. Count the chargeable days (§3.1).
2. Apply the grace period (§3.3). If the fine is zero, stop.
3. Multiply the chargeable days by the daily rate (§3.2).
4. Apply the senior discount, if applicable (§3.4).
5. Round (§3.5).
6. Apply the maximum (§3.6).

### 3.8 Worked examples

| # | Member | Material | Due date | Returned | Chargeable days | Calculation | Fine |
|---|---|---|---|---|---|---|---|
| 1 | ADULT | BOOK | Mon 2026-03-02 | Tue 2026-03-03 | 1 | Within grace period | €0.00 |
| 2 | ADULT | BOOK | Mon 2026-03-02 | Wed 2026-03-04 | 2 | 2 × €0.25 | €0.50 |
| 3 | ADULT | BOOK | Fri 2026-03-06 | Fri 2026-03-13 | 7 | 7 × €0.25 | €1.75 |
| 4 | ADULT | BOOK | Sat 2026-04-04 | Wed 2026-04-08 | 2 | Sunday 5 April and Easter Monday 6 April not counted; 2 × €0.25 | €0.50 |
| 5 | SENIOR | BOOK | Mon 2026-03-02 | Sat 2026-03-07 | 5 | 5 × €0.25 = €1.25; minus 50% = €0.625; rounded | €0.63 |
| 6 | ADULT | AUDIOVISUAL (replacement cost €8.00) | Mon 2026-03-02 | Mon 2026-03-16 | 12 | 12 × €1.00 = €12.00; maximum is the lower of €10.00 and €8.00 | €8.00 |

### 3.9 Accrued fine

While a loan is overdue and not yet returned, the member portal and the staff console show the fine that would be charged if the copy were returned today. This amount is informative; the fine becomes a charge on the account only when the copy is returned or declared lost.

## 4. Lost items

### 4.1 Lost declaration

A copy is declared lost when it has not been returned **42 days after its due date** (BR-FIN-07).

### 4.2 Consequences

- The copy status becomes LOST.
- The overdue fine stops accruing. It is fixed at the amount calculated on the day of the declaration (including the maximum of §3.6) and added to the account as a charge.
- The member is charged the **replacement cost** of the copy and an **administration fee of €5.00**.

### 4.3 Lost copy returned

If a lost copy is returned within 90 days of the lost declaration, the replacement cost is refunded, or cancelled if it was not yet paid. The overdue fine and the administration fee remain due (BR-FIN-10).

## 5. Waivers

Staff can waive a charge fully or partially. A reason is always mandatory. The limits per role are described in FA-09 §4. Every waiver is recorded and reported (FA-08, R-04).

## 6. Payments

### 6.1 Payment methods

- At the desk: cash or debit card.
- Online in the member portal via PayBridge.

### 6.2 Partial payments

Partial payments are allowed at the desk. Online, the member pays either the full balance or a selection of charges.

### 6.3 Allocation

Desk payments are allocated to the oldest outstanding charge first. Online payments are allocated to the charges selected by the member (BR-FIN-09).

### 6.4 Receipts

A receipt is sent by e-mail for every payment (N-10). Desk payments can also be printed.

## 7. Account balance

The outstanding balance of a member is the sum of all unpaid overdue fines, replacement costs and administration fees. The balance determines whether a member is blocked (FA-02 §5).
