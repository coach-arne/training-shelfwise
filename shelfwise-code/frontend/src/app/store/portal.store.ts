import { signal } from '@angular/core';
import { portalApi } from '../core/api/portal.api';
import { AccountView, LoanView, MemberDetail, ReservationView } from '../core/models';

/** The logged-in member's own data in the portal. */
const member = signal<MemberDetail | null>(null);
const loans = signal<LoanView[]>([]);
const history = signal<LoanView[]>([]);
const reservations = signal<ReservationView[]>([]);
const account = signal<AccountView | null>(null);

export const portalMember = member.asReadonly();
export const portalLoans = loans.asReadonly();
export const portalHistory = history.asReadonly();
export const portalReservations = reservations.asReadonly();
export const portalAccount = account.asReadonly();

export async function refreshPortal(): Promise<void> {
  const [detail, openLoans, pastLoans, memberReservations, memberAccount] = await Promise.all([
    portalApi.me(),
    portalApi.loans(),
    portalApi.history(),
    portalApi.reservations(),
    portalApi.account(),
  ]);
  member.set(detail);
  loans.set(openLoans);
  history.set(pastLoans);
  reservations.set(memberReservations);
  account.set(memberAccount);
}

export function clearPortal(): void {
  member.set(null);
  loans.set([]);
  history.set([]);
  reservations.set([]);
  account.set(null);
}
