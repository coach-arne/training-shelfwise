import { formatDate, formatMoney, initialsOf, toIsoDate } from './format';

describe('format helpers', () => {
  it('shows a calendar date without a time zone shift', () => {
    expect(formatDate('2026-03-16')).toBe('16 Mar 2026');
    expect(formatDate(null)).toBe('');
  });

  it('formats money in euro with two decimals', () => {
    expect(formatMoney(0.5)).toContain('0.50');
    expect(formatMoney(null)).toContain('0.00');
  });

  it('converts a date to the ISO calendar date', () => {
    expect(toIsoDate(new Date(2026, 2, 2))).toBe('2026-03-02');
  });

  it('takes at most two initials of a name', () => {
    expect(initialsOf('Sam Stone')).toBe('SS');
    expect(initialsOf('admin')).toBe('A');
    expect(initialsOf('Jan van der Berg')).toBe('JV');
    expect(initialsOf('')).toBe('');
  });
});
