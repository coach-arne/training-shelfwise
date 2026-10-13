# TA-05 — REST API Design

| Field | Value |
|---|---|
| Document ID | TA-05 |
| Version | 1.1 |
| Status | Approved |
| Owner | Jonas Becker, Backend Lead |
| Last updated | 2026-05-05 |
| Related | TA-02, TA-06, FA-04 to FA-06 |

## 1. Conventions

- Base path: `/api/v1`.
- JSON request and response bodies; field names in camelCase.
- Resource names are plural nouns.
- Dates as ISO 8601 strings (YYYY-MM-DD); timestamps in UTC with offset.
- Amounts are strings with two decimals plus a currency field, for example amount "2.50" and currency "EUR".
- Errors as problem details (RFC 9457) with the error codes listed in TA-02 §8.
- Lists are paginated with the query parameters `page` and `size`.
- Authentication with a bearer access token issued by Keycloak (TA-06). Endpoints marked "public" require no authentication.
- "own" means a member may only access their own data.

## 2. Endpoints

### 2.1 Members

| Method | Path | Description | Roles |
|---|---|---|---|
| GET | /api/v1/members | Search members (name, card number, date of birth) | LIBRARIAN, BRANCH_MANAGER |
| POST | /api/v1/members | Register a member | LIBRARIAN, BRANCH_MANAGER |
| GET | /api/v1/members/{id} | Member details, including blocked status | LIBRARIAN, BRANCH_MANAGER, MEMBER (own) |
| PATCH | /api/v1/members/{id} | Update member data | LIBRARIAN, BRANCH_MANAGER, MEMBER (own, contact details and preferences only) |
| GET | /api/v1/members/{id}/loans | Current loans and loan history | LIBRARIAN, BRANCH_MANAGER, MEMBER (own) |
| GET | /api/v1/members/{id}/charges | Charges and account balance | LIBRARIAN, BRANCH_MANAGER, MEMBER (own) |

### 2.2 Catalogue

| Method | Path | Description | Roles |
|---|---|---|---|
| GET | /api/v1/titles | Search titles | public |
| GET | /api/v1/titles/{id}/availability | Available copies per branch and number of waiting reservations | public |
| POST | /api/v1/titles | Create a title | LIBRARIAN, BRANCH_MANAGER |
| POST | /api/v1/copies | Add a copy | LIBRARIAN, BRANCH_MANAGER |
| PATCH | /api/v1/copies/{id} | Update a copy (for example status DAMAGED) | LIBRARIAN, BRANCH_MANAGER |

### 2.3 Lending

| Method | Path | Description | Roles |
|---|---|---|---|
| POST | /api/v1/loans | Checkout (member ID and copy barcode) | LIBRARIAN, BRANCH_MANAGER |
| POST | /api/v1/loans/{id}/return | Return a copy | LIBRARIAN, BRANCH_MANAGER |
| POST | /api/v1/loans/{id}/renew | Renew a loan | LIBRARIAN, BRANCH_MANAGER, MEMBER (own) |
| GET | /api/v1/loans/{id}/accrued-fine | Fine if the copy were returned today | LIBRARIAN, BRANCH_MANAGER, MEMBER (own) |

### 2.4 Reservations

| Method | Path | Description | Roles |
|---|---|---|---|
| POST | /api/v1/reservations | Place a reservation | LIBRARIAN, BRANCH_MANAGER, MEMBER (own) |
| DELETE | /api/v1/reservations/{id} | Cancel a reservation | LIBRARIAN, BRANCH_MANAGER, MEMBER (own) |
| GET | /api/v1/titles/{id}/reservations | Reservation queue of a title | LIBRARIAN, BRANCH_MANAGER |

### 2.5 Charges and payments

| Method | Path | Description | Roles |
|---|---|---|---|
| POST | /api/v1/charges/{id}/waive | Waive a charge fully or partially (amount and reason) | LIBRARIAN, BRANCH_MANAGER |
| POST | /api/v1/payments | Register a desk payment | LIBRARIAN, BRANCH_MANAGER |
| POST | /api/v1/payments/online | Start a PayBridge checkout for the full balance or selected charges | MEMBER (own) |
| POST | /api/v1/payments/paybridge-webhook | Payment status callback from PayBridge | PayBridge (signature verified) |

### 2.6 Reports and administration

| Method | Path | Description | Roles |
|---|---|---|---|
| GET | /api/v1/reports/{reportId} | Run a report (R-01 to R-07), with format csv or pdf | Per FA-09 |
| GET, PUT | /api/v1/admin/tariffs | Read and update the fine tariff table | ADMINISTRATOR |
| GET, PUT | /api/v1/admin/closing-days | Read and update closing days | ADMINISTRATOR |

## 3. Behaviour of selected endpoints

### 3.1 Checkout

Returns 201 with the loan and its due date. Possible errors: MEMBER_BLOCKED, LOAN_LIMIT_REACHED, COPY_NOT_AVAILABLE.

### 3.2 Renew

The new due date is calculated as the **renewal date plus the loan period** of the material type, adjusted to the next opening day if needed. Returns 200 with the updated loan. If the renewal is refused, the response is 409 RENEWAL_NOT_ALLOWED with one of the reason codes RESERVATIONS_WAITING, MEMBER_BLOCKED, LOAN_OVERDUE or MAX_RENEWALS_REACHED.

### 3.3 Return

Returns 200 with the closed loan, the charge created (if any) and the new copy status. The return date is the current date, unless a book-drop date is passed by the staff console.

### 3.4 Accrued fine

Calculated on demand by the FineCalculator (ADR-005). Response: amount, number of chargeable days, and whether the grace period applies.

### 3.5 Waive

Role-specific limits are enforced server-side (see TA-06 §4). Returns 200 with the updated charge.

### 3.6 PayBridge webhook

The signature of the request is verified with the shared secret. Processing is idempotent based on the PayBridge reference: a callback for a reference that was already processed is acknowledged without creating a second payment.

## 4. Versioning

Backward-compatible changes are added to `/api/v1`. Breaking changes require a new base path `/api/v2`, with a transition period agreed with the frontend team.
