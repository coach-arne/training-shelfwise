import { apiGet, apiPost, apiPut } from '../http';
import { AccountView, LoanView, MemberDetail, OnlinePaymentView, PreferencesRequest, ReservationView } from '../models';

export const portalApi = {
  me: () => apiGet<MemberDetail>('/api/portal/me'),
  updatePreferences: (request: PreferencesRequest) => apiPut<MemberDetail>('/api/portal/me/preferences', request),
  loans: () => apiGet<LoanView[]>('/api/portal/loans'),
  history: () => apiGet<LoanView[]>('/api/portal/loans/history'),
  renew: (loanId: number) => apiPost<LoanView>(`/api/portal/loans/${loanId}/renew`),
  account: () => apiGet<AccountView>('/api/portal/account'),
  reservations: () => apiGet<ReservationView[]>('/api/portal/reservations'),
  reserve: (titleId: number, pickupBranchId: number) =>
    apiPost<ReservationView>('/api/portal/reservations', { titleId, pickupBranchId }),
  cancelReservation: (id: number) => apiPost<ReservationView>(`/api/portal/reservations/${id}/cancel`),
  startPayment: (chargeIds: number[]) => apiPost<OnlinePaymentView>('/api/portal/payments', { chargeIds }),
  payment: (reference: string) => apiGet<OnlinePaymentView>(`/api/portal/payments/${reference}`),
};

export const providerApi = {
  payment: (reference: string) => apiGet<OnlinePaymentView>(`/api/provider/payments/${reference}`),
  report: (reference: string, result: string) =>
    apiPost<OnlinePaymentView>(`/api/provider/payments/${reference}/result`, { result }),
};
