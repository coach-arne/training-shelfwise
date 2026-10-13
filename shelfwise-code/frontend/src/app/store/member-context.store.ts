import { computed, signal } from '@angular/core';
import { deskApi } from '../core/api/desk.api';
import { AccountView, LoanView, MemberDetail, ReservationView } from '../core/models';

/**
 * The member the librarian is serving at the desk: details, open loans, reservations and account.
 * Every screen of the desk workspace reads from here and calls refreshMemberContext() after a change.
 */
const member = signal<MemberDetail | null>(null);
const loans = signal<LoanView[]>([]);
const reservations = signal<ReservationView[]>([]);
const account = signal<AccountView | null>(null);
const loading = signal(false);

export const contextMember = member.asReadonly();
export const contextLoans = loans.asReadonly();
export const contextReservations = reservations.asReadonly();
export const contextAccount = account.asReadonly();
export const contextLoading = loading.asReadonly();
export const hasContextMember = computed(() => member() !== null);

async function load(id: number): Promise<void> {
  loading.set(true);
  try {
    const [detail, openLoans, memberReservations, memberAccount] = await Promise.all([
      deskApi.member(id),
      deskApi.loans(id),
      deskApi.reservations(id),
      deskApi.account(id),
    ]);
    member.set(detail);
    loans.set(openLoans);
    reservations.set(memberReservations);
    account.set(memberAccount);
  } finally {
    loading.set(false);
  }
}

export function selectMember(id: number): Promise<void> {
  return load(id);
}

export async function refreshMemberContext(): Promise<void> {
  const current = member();
  if (current) {
    await load(current.id);
  }
}

export function clearMemberContext(): void {
  member.set(null);
  loans.set([]);
  reservations.set([]);
  account.set(null);
}
