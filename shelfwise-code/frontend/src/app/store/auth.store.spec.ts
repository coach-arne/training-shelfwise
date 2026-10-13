import { homeRoute, isDesk, logout } from './auth.store';

describe('auth store', () => {
  beforeEach(() => logout());

  it('is logged out after logout()', () => {
    expect(isDesk()).toBeFalse();
  });

  it('sends every role to its own start page (AC-ROL-02: administrators have no desk)', () => {
    expect(homeRoute('MEMBER')).toBe('/portal');
    expect(homeRoute('LIBRARIAN')).toBe('/staff/desk');
    expect(homeRoute('BRANCH_MANAGER')).toBe('/staff/desk');
    expect(homeRoute('ADMINISTRATOR')).toBe('/staff/admin');
    expect(homeRoute(null)).toBe('/catalogue');
  });
});
