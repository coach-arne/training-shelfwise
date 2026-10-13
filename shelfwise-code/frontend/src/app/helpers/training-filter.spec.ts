import { DemoAccount, DemoBook } from '../core/models';
import { filterAccounts, filterBooks } from './training-filter';

const ada: DemoAccount = {
  type: 'MEMBER', id: 1, username: 'NPL000001', displayName: 'Ada Adams', role: 'MEMBER', branchCode: null,
  scenario: 'S-01', description: 'Happy path: no loans, no charges',
};
const hugo: DemoAccount = {
  type: 'MEMBER', id: 8, username: 'NPL000008', displayName: 'Hugo Hale', role: 'MEMBER', branchCode: null,
  scenario: 'S-08', description: 'SENIOR with accrued fine 1.50',
};
const atlas: DemoBook = {
  titleId: 1, title: "The Lighthouse Keeper's Atlas", authors: 'Marian Holloway', isbn: '9781000000001',
  materialType: 'BOOK', copies: [{ barcode: 'CEN-0001', status: 'AVAILABLE', currentBranchCode: 'CEN' }],
};
const bridges: DemoBook = {
  titleId: 2, title: 'A Brief History of Bridges', authors: 'Hector Dunmore', isbn: null,
  materialType: 'BOOK', copies: [{ barcode: 'RIV-0007', status: 'ON_LOAN', currentBranchCode: 'RIV' }],
};

describe('training filters', () => {
  it('returns everything for an empty query', () => {
    expect(filterAccounts([ada, hugo], '  ')).toEqual([ada, hugo]);
    expect(filterBooks([atlas, bridges], '')).toEqual([atlas, bridges]);
  });

  it('finds accounts by scenario code, name, card number or description', () => {
    expect(filterAccounts([ada, hugo], 's-08')).toEqual([hugo]);
    expect(filterAccounts([ada, hugo], 'ada')).toEqual([ada]);
    expect(filterAccounts([ada, hugo], 'NPL000008')).toEqual([hugo]);
    expect(filterAccounts([ada, hugo], 'accrued fine')).toEqual([hugo]);
  });

  it('finds books by title, author, ISBN or barcode, with every word required', () => {
    expect(filterBooks([atlas, bridges], 'lighthouse atlas')).toEqual([atlas]);
    expect(filterBooks([atlas, bridges], 'dunmore')).toEqual([bridges]);
    expect(filterBooks([atlas, bridges], '9781000000001')).toEqual([atlas]);
    expect(filterBooks([atlas, bridges], 'riv-0007')).toEqual([bridges]);
    expect(filterBooks([atlas, bridges], 'lighthouse bridges')).toEqual([]);
  });
});
