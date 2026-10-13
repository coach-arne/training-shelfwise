# TA-06 — Security and Privacy

| Field | Value |
|---|---|
| Document ID | TA-06 |
| Version | 1.0 |
| Status | Approved |
| Owner | Pieter van Dam, Data Protection Officer |
| Last updated | 2026-04-24 |
| Related | FA-02, FA-09, TA-01, TA-05 |

## 1. Identity and access management

- Keycloak realm `shelfwise`.
- Two public OIDC clients, `member-portal` and `staff-console`, both using the authorisation code flow with PKCE.
- The backend is an OAuth2 resource server and validates the signature, issuer, audience and expiry of every access token.
- Staff accounts are federated from the city's Active Directory. Member accounts are created in Keycloak when the member activates portal access, and are linked to the card number.
- Multi-factor authentication is mandatory for BRANCH_MANAGER and ADMINISTRATOR.

## 2. Roles

Realm roles: MEMBER, LIBRARIAN, BRANCH_MANAGER, ADMINISTRATOR (see FA-09). Staff tokens contain a `branch` claim with the code of the staff member's branch.

## 3. Authorisation

- Role checks are applied at method level in the application services, not only in the controllers.
- For MEMBER, every request is additionally checked for ownership: the member ID in the request must match the member linked to the token.
- Branch-scoped functions (for example reports R-01 and R-02 for librarians) use the `branch` claim.

## 4. Sensitive operations

| Operation | Rule |
|---|---|
| Waiving charges | Librarians and Branch Managers may waive charges of any amount. A reason is mandatory and every waiver is recorded in the audit log. |
| Desk payments | Recorded with the staff user ID; cash totals are reconciled per branch per day. |
| Changes to tariffs and closing days | ADMINISTRATOR only; recorded in the audit log with old and new values. |
| Role assignment | ADMINISTRATOR only, in Keycloak; reviewed quarterly. |

## 5. Personal data and privacy

A DPIA has been carried out for Shelfwise. Main measures:

| Data | Measure |
|---|---|
| Member identity and contact data | Accessible to staff for lending purposes only; deleted when the membership is closed (FA-02 §8) |
| Date of birth | Used to derive the member category; visible to staff only |
| Loan history | Retained indefinitely, so that a recommendation feature can be offered in a later phase |
| Charges and payments | Retained 7 years for accounting purposes |
| Notification log | Retained 12 months |

General measures:

- TLS 1.2 or higher for all connections.
- Database storage volumes and backups are encrypted.
- No production data in non-production environments; migration rehearsals use masked data.

## 6. Audit logging

- Audit events are written for: charges, waivers, payments, refunds, lost declarations, changes to configuration, and role changes.
- Each event contains the time, the actor, the action, the entity and the relevant details.
- Audit events cannot be changed or deleted through the application. Retention: 7 years.

## 7. Secure development

- OWASP ASVS level 2 as the reference.
- Static analysis and dependency scanning in every pipeline run; high and critical findings block the release.
- Annual penetration test, and one before go-live.
