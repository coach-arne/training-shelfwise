# FA-03 — Catalogue and Copies

| Field | Value |
|---|---|
| Document ID | FA-03 |
| Version | 1.0 |
| Status | Approved |
| Owner | Tom Hollis, Business Analyst |
| Last updated | 2026-03-20 |
| Related | FA-04, FA-05, FA-06, TA-04 |

## 1. Titles and copies

A **title** is the bibliographic description of a work: title, author(s), publisher, year, ISBN (where applicable) and material type. A **copy** is one physical item of a title, identified by a unique barcode. A title can have many copies spread over several branches.

Loans, holds and statuses always apply to a **copy**. Reservations are placed on a **title** (see FA-05).

Titles and copies are migrated from LibTrack 4 and maintained in Shelfwise by librarians and branch managers.

## 2. Material types

| Material type | Examples | Loan period | Default replacement cost |
|---|---|---|---|
| BOOK | Novels, non-fiction, comics, graphic novels | 21 days | €25.00 |
| AUDIOVISUAL | DVD, Blu-ray, audio CD | 7 days | €20.00 |
| MAGAZINE | Back issues of periodicals | 14 days | €6.00 |

The loan periods are repeated here for convenience; FA-04 §2 is the reference.

## 3. Copy attributes

| Attribute | Description |
|---|---|
| Barcode | Unique identifier printed on the copy |
| Title | The title this copy belongs to |
| Home branch | Branch that owns the copy |
| Current branch | Branch where the copy is physically located |
| Shelf mark | Location code on the shelf |
| Status | See §4 |
| Replacement cost | Amount charged if the copy is lost. If empty, the default for the material type applies (§2) |
| Acquisition date | Date the copy was added to the collection |

## 4. Copy statuses

| Status | Meaning |
|---|---|
| AVAILABLE | On the shelf and available for loan |
| ON_LOAN | Borrowed by a member |
| ON_HOLD | Set aside at a branch for a member who reserved the title |
| IN_TRANSIT | Being transferred between branches |
| LOST | Declared lost (see FA-06 §4) |
| DAMAGED | Temporarily withdrawn for repair |
| WITHDRAWN | Permanently removed from the collection |

### 4.1 Status transitions

| From | To | Trigger |
|---|---|---|
| AVAILABLE | ON_LOAN | Checkout |
| ON_HOLD | ON_LOAN | Checkout by the member for whom it is held |
| ON_LOAN | AVAILABLE | Return at the home branch, no reservation waiting |
| ON_LOAN | IN_TRANSIT | Return at another branch, or return when a reservation is waiting at another pickup branch |
| ON_LOAN | ON_HOLD | Return at the pickup branch of the first waiting reservation |
| IN_TRANSIT | AVAILABLE / ON_HOLD | Arrival at the destination branch |
| ON_HOLD | AVAILABLE / ON_HOLD | Hold expires; copy goes to the next reservation or back to the shelf |
| ON_LOAN | LOST | Lost declaration (nightly job) |
| LOST | AVAILABLE | Copy is found or returned |
| Any | DAMAGED / WITHDRAWN | Manual action by staff |

## 5. Catalogue search

- Members and anonymous visitors can search the catalogue by title, author, ISBN and keyword.
- Results show availability per branch (number of available copies) and the number of waiting reservations.
- Staff can additionally search by barcode and see the status and current branch of every copy.
