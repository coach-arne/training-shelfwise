# TA-04 — Data Model

| Field | Value |
|---|---|
| Document ID | TA-04 |
| Version | 1.1 |
| Status | Approved |
| Owner | Jonas Becker, Backend Lead |
| Last updated | 2026-04-28 |
| Related | TA-02, FA-02 to FA-06, ADR-004 |

## 1. Conventions

- Database: PostgreSQL 16 (ADR-004). Schema changes are managed with Flyway.
- Table and column names in snake_case, table names in singular.
- Primary keys: `id`, BIGINT generated as identity.
- Every table has `created_at` and `updated_at` (timestamptz, UTC). Tables that are updated concurrently have a `version` column for optimistic locking.
- Monetary amounts are stored as **INTEGER in euro cents** (for example, €2.50 is stored as 250) to avoid rounding problems. Column names end in `_cents`.
- Enumerations are stored as VARCHAR with a check constraint.

## 2. Entity overview

- A **member** may have a guardian, who is also a member.
- A **title** has one or more **copies**. Every copy has a home branch and a current branch.
- A **loan** always concerns exactly one copy and one member.
- A **reservation** is placed by a member on a title. When a copy is assigned, the reservation refers to that copy.
- A **charge** is an amount owed by a member, optionally linked to a loan.
- A **payment** is allocated to one or more charges via **payment_allocation**.
- A **waiver** reduces a charge.

## 3. Tables

### 3.1 member

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| card_number | VARCHAR(20) | Unique |
| first_name, last_name | VARCHAR(100) | |
| date_of_birth | DATE | |
| email | VARCHAR(255) | |
| phone | VARCHAR(30) | Nullable |
| street, postal_code, city | VARCHAR | |
| category | VARCHAR(10) | ADULT, YOUTH, SENIOR |
| guardian_member_id | BIGINT | FK member, nullable |
| guardian_name, guardian_email | VARCHAR | When the guardian is not a member |
| membership_valid_until | DATE | |
| keep_reading_history | BOOLEAN | Opt-in, default false |
| sms_opt_in | BOOLEAN | Default false |
| keycloak_user_id | VARCHAR(64) | Nullable until the portal account is activated |
| version | INTEGER | |

The blocked status is not stored; it is derived from the balance and the membership validity.

### 3.2 branch

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| code | VARCHAR(10) | Unique, for example CEN, RIV |
| name | VARCHAR(100) | |

### 3.3 title

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| isbn | VARCHAR(17) | Nullable |
| title | VARCHAR(500) | |
| authors | VARCHAR(500) | |
| publisher | VARCHAR(200) | |
| publication_year | SMALLINT | |
| material_type | VARCHAR(12) | BOOK, AUDIOVISUAL, MAGAZINE |

### 3.4 copy

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| barcode | VARCHAR(20) | Unique |
| title_id | BIGINT | FK title |
| home_branch_id | BIGINT | FK branch |
| current_branch_id | BIGINT | FK branch |
| shelf_mark | VARCHAR(50) | |
| status | VARCHAR(12) | AVAILABLE, ON_LOAN, ON_HOLD, IN_TRANSIT, LOST, DAMAGED, WITHDRAWN |
| replacement_cost_cents | INTEGER | Nullable; default per material type applies |
| acquired_on | DATE | |
| version | INTEGER | |

### 3.5 loan

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| member_id | BIGINT | FK member; set to NULL when anonymised |
| title_id | BIGINT | FK title |
| checkout_branch_id | BIGINT | FK branch |
| checkout_date | DATE | |
| due_date | DATE | |
| renewal_count | SMALLINT | 0 to 2 |
| return_date | DATE | Nullable while the loan is open |
| return_branch_id | BIGINT | FK branch, nullable |
| anonymised | BOOLEAN | Default false |
| version | INTEGER | |

### 3.6 reservation

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| title_id | BIGINT | FK title |
| member_id | BIGINT | FK member |
| pickup_branch_id | BIGINT | FK branch |
| status | VARCHAR(10) | WAITING, ON_HOLD, COLLECTED, EXPIRED, CANCELLED |
| held_copy_id | BIGINT | FK copy, nullable |
| hold_until | DATE | Nullable |
| version | INTEGER | |

The queue order is determined by `created_at`.

### 3.7 charge

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| member_id | BIGINT | FK member |
| loan_id | BIGINT | FK loan, nullable |
| type | VARCHAR(20) | OVERDUE_FINE, LOST_REPLACEMENT, LOST_ADMIN_FEE |
| amount_cents | INTEGER | Original amount |
| outstanding_cents | INTEGER | Remaining amount after payments and waivers |
| status | VARCHAR(16) | OPEN, PARTIALLY_PAID, PAID, WAIVED, REFUNDED |
| charged_on | DATE | |
| version | INTEGER | |

### 3.8 payment and payment_allocation

**payment**

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| member_id | BIGINT | FK member |
| amount_cents | INTEGER | |
| method | VARCHAR(10) | CASH, CARD, ONLINE |
| paybridge_reference | VARCHAR(64) | Unique, nullable; for online payments |
| staff_user_id | VARCHAR(64) | Keycloak subject of the librarian; null for online payments |
| received_at | TIMESTAMPTZ | |

**payment_allocation**

| Column | Type | Notes |
|---|---|---|
| payment_id | BIGINT | PK part, FK payment |
| charge_id | BIGINT | PK part, FK charge |
| amount_cents | INTEGER | |

### 3.9 waiver

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | PK |
| charge_id | BIGINT | FK charge |
| amount_cents | INTEGER | |
| reason | VARCHAR(500) | Mandatory |
| staff_user_id | VARCHAR(64) | |
| waived_at | TIMESTAMPTZ | |

### 3.10 Configuration tables

**fine_tariff**: `member_category`, `material_type`, `daily_rate_cents`, `valid_from`. Primary key: (`member_category`, `material_type`, `valid_from`). The rate that applies is the one with the most recent `valid_from` on or before the calculation date.

**closing_day**: `day` (DATE, PK), `description`. Sundays are not stored; they are always closing days.

### 3.11 Supporting tables

- **audit_event**: `id`, `occurred_at`, `actor`, `action`, `entity_type`, `entity_id`, `details` (jsonb).
- **notification_log**: `id`, `member_id`, `notification_type` (N-01 to N-10), `channel`, `sent_at`, `delivery_status`.

## 4. Important constraints and indexes

| Constraint / index | Purpose |
|---|---|
| Unique partial index on `loan(copy_id)` where `return_date` is null | A copy can never be on loan twice at the same time |
| Index on `loan(due_date)` where `return_date` is null | Fast selection of overdue loans by the nightly jobs |
| Index on `reservation(title_id, status, created_at)` | Queue processing |
| Index on `charge(member_id, status)` | Fast calculation of the account balance |
| Unique index on `payment(paybridge_reference)` | Idempotent processing of PayBridge webhooks |

## 5. Anonymisation

Anonymisation of a loan sets `member_id` to NULL and `anonymised` to true, according to FA-02 §7. Loans with open charges are not anonymised.
