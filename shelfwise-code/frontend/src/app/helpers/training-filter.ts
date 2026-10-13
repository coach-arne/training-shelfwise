import { DemoAccount, DemoBook } from '../core/models';

function normalise(text: string): string {
  return text.toLowerCase().trim();
}

/** True when every word of the query occurs somewhere in the haystack. */
function matchesAll(haystack: string, query: string): boolean {
  const words = normalise(query).split(/\s+/).filter(Boolean);
  const text = normalise(haystack);
  return words.every((word) => text.includes(word));
}

/** Accounts whose user name, name, scenario code or description contain every word of the query. */
export function filterAccounts(accounts: DemoAccount[], query: string): DemoAccount[] {
  return accounts.filter((account) =>
    matchesAll([account.username, account.displayName, account.scenario, account.description, account.branchCode].join(' '), query));
}

/** Books whose title, authors, ISBN or one of the barcodes contain every word of the query. */
export function filterBooks(books: DemoBook[], query: string): DemoBook[] {
  return books.filter((book) =>
    matchesAll([book.title, book.authors, book.isbn, ...book.copies.map((copy) => copy.barcode)].join(' '), query));
}
