import { ageOn, categoryOn, MEMBERSHIP_FEE } from './membership';

describe('membership helpers (F-MEM-01, BR-MEM-04)', () => {
  it('AC-MEM-07: someone born 2008-06-15 is YOUTH on 2026-03-02 and ADULT on 2027-03-02', () => {
    expect(categoryOn('2008-06-15', new Date(2026, 2, 2))).toBe('YOUTH');
    expect(categoryOn('2008-06-15', new Date(2027, 2, 2))).toBe('ADULT');
  });

  it('turns 18 on the birthday', () => {
    expect(categoryOn('2008-03-02', new Date(2026, 2, 1))).toBe('YOUTH');
    expect(categoryOn('2008-03-02', new Date(2026, 2, 2))).toBe('ADULT');
  });

  it('turns SENIOR at 65', () => {
    expect(categoryOn('1961-03-02', new Date(2026, 2, 1))).toBe('ADULT');
    expect(categoryOn('1961-03-02', new Date(2026, 2, 2))).toBe('SENIOR');
  });

  it('counts the completed years', () => {
    expect(ageOn('2000-12-31', new Date(2026, 0, 1))).toBe(25);
  });

  it('has the fees of BR-MEM-04', () => {
    expect(MEMBERSHIP_FEE).toEqual({ YOUTH: 0, ADULT: 12, SENIOR: 6 });
  });
});
