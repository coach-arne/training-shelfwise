import { computed, signal } from '@angular/core';
import { authApi } from '../core/api/auth.api';
import { LoginResult, Role, UserProfile } from '../core/models';

const STORAGE_KEY = 'shelfwise.session';

interface Session {
  token: string;
  profile: UserProfile;
}

function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

const session = signal<Session | null>(loadSession());

export const authSession = session.asReadonly();
export const authToken = computed(() => session()?.token ?? null);
export const profile = computed(() => session()?.profile ?? null);
export const isLoggedIn = computed(() => session() !== null);
export const role = computed<Role | null>(() => session()?.profile.role ?? null);
export const isMember = computed(() => role() === 'MEMBER');
export const isStaff = computed(() => role() !== null && role() !== 'MEMBER');
/** Librarians and branch managers do the lending and financial desk work; administrators never do. */
export const isDesk = computed(() => role() === 'LIBRARIAN' || role() === 'BRANCH_MANAGER');
export const isAdmin = computed(() => role() === 'ADMINISTRATOR');
export const canWaiveMoreThanLimit = computed(() => role() === 'BRANCH_MANAGER');

function store(result: LoginResult): UserProfile {
  const value: Session = { token: result.token, profile: result.profile };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
  session.set(value);
  return result.profile;
}

export async function login(username: string, password: string): Promise<UserProfile> {
  return store(await authApi.login(username, password));
}

export async function demoLogin(type: string, id: number): Promise<UserProfile> {
  return store(await authApi.demoLogin(type, id));
}

export function logout(): void {
  localStorage.removeItem(STORAGE_KEY);
  session.set(null);
}

/** Where a user lands after logging in. */
export function homeRoute(userRole: Role | null): string {
  switch (userRole) {
    case 'MEMBER':
      return '/portal';
    case 'ADMINISTRATOR':
      return '/staff/admin';
    case 'LIBRARIAN':
    case 'BRANCH_MANAGER':
      return '/staff/desk';
    default:
      return '/catalogue';
  }
}
