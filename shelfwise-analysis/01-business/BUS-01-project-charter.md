# BUS-01 — Project Charter

| Field | Value |
|---|---|
| Document ID | BUS-01 |
| Version | 1.1 |
| Status | Approved |
| Owner | Hannah Mercer, Product Owner |
| Last updated | 2026-03-02 |
| Approved by | Margaret Doyle, Library Director |

## 1. Background

Northwick Public Library (NPL) operates six branches: Central, Riverside, Eastgate, Millbrook, Harbour and Westfield. The library serves approximately **85,000 active members** and manages a collection of about **410,000 copies**. Annual circulation is around 1.1 million loans.

Lending is currently supported by LibTrack 4, a vendor product introduced in 2014. The vendor has announced that support for LibTrack 4 ends on **31 March 2027**. LibTrack 4 has no self-service portal, fine rules are hard-coded, and changes require paid vendor work.

## 2. Objectives

1. Replace LibTrack 4 before vendor support ends.
2. Offer members a self-service portal for renewals, reservations, account overview and online payment.
3. Make the fine policy transparent and configurable, so that members can always see what they owe and why.
4. Reduce the time staff spend at the desk on routine transactions.
5. Comply with data protection regulation (GDPR), in particular regarding reading history.

## 3. Scope

### 3.1 In scope (phase 1)

- Membership management (registration, categories, renewal of membership)
- Title and copy management, as far as needed for lending
- Loans, returns and renewals, at the desk and via the member portal
- Reservations and holds
- Fines, lost items, waivers and payments (desk and online)
- Notifications to members (e-mail, optional SMS)
- Operational reporting for branch managers and finance
- Migration of members, titles, copies, open loans and open charges from LibTrack 4

### 3.2 Out of scope (phase 1)

- Inter-library loans (planned for phase 2)
- E-books and other digital lending
- Acquisitions and purchasing
- Events and room booking
- Self-service kiosk hardware (see open issue OI-04)

## 4. Stakeholders

| Name | Role in project |
|---|---|
| Margaret Doyle | Library Director — Sponsor |
| Hannah Mercer | Head of Public Services — Product Owner |
| Ellen Ward | Branch Manager, Central — Key user representing branches |
| Raj Malhotra | Finance Controller — Owner of fine tariffs and payment reconciliation |
| Pieter van Dam | Data Protection Officer — Privacy and security sign-off |
| Tom Hollis | Business Analyst |
| Sara Lindqvist | Lead Architect |
| Jonas Becker | Backend Lead |
| Mira Okafor | Frontend Lead |

## 5. Milestones

| Milestone | Target |
|---|---|
| Analysis completed | May 2026 |
| Build | June – November 2026 |
| User acceptance testing | December 2026 |
| Go-live (all branches at once) | January 2027 |
| LibTrack 4 decommissioned | March 2027 |

## 6. Success criteria

- At least 40% of renewals are done via the member portal within six months after go-live.
- Average desk time per checkout below 30 seconds.
- No fine disputes caused by calculation errors in the system during the first six months.
- DPIA approved by the Data Protection Officer before go-live.

## 7. Main risks

| Risk | Mitigation |
|---|---|
| Data quality of LibTrack 4 migration | Two migration rehearsals in the acceptance environment |
| Late changes to the fine policy | Tariffs configurable; dedicated fines workshop in April 2026 |
| Staff adoption | Key users per branch, train-the-trainer approach |
| Go-live slips past end of LibTrack 4 support | Go-live planned three months before end of support |
