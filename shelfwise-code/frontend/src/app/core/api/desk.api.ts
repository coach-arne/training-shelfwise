import { apiGet, apiPost, apiPut } from '../http';
import {
  AccountView, ArrivalReceipt, CheckoutReceipt, ChargeView, LoanView, MemberDetail, MemberSummary, PaymentMethod,
  PaymentReceipt, RegisterMemberRequest, RegistrationResult, RenewalResult, ReservationView, ReturnReceipt,
  StaffUpdateMemberRequest,
} from '../models';

export const deskApi = {
  searchMembers: (q: string) => apiGet<MemberSummary[]>('/api/staff/members', { q }),
  duplicates: (firstName: string, lastName: string, birthDate: string) =>
    apiGet<MemberSummary[]>('/api/staff/members/duplicates', { firstName, lastName, birthDate }),
  member: (id: number) => apiGet<MemberDetail>(`/api/staff/members/${id}`),
  memberByCard: (cardNumber: string) => apiGet<MemberDetail>(`/api/staff/members/by-card/${encodeURIComponent(cardNumber)}`),
  register: (request: RegisterMemberRequest) => apiPost<RegistrationResult>('/api/staff/members', request),
  updateMember: (id: number, request: StaffUpdateMemberRequest) => apiPut<MemberDetail>(`/api/staff/members/${id}`, request),
  renewMembership: (id: number, paymentMethod: PaymentMethod) =>
    apiPost<RenewalResult>(`/api/staff/members/${id}/renew`, { paymentMethod }),

  checkout: (cardNumber: string, barcode: string) => apiPost<CheckoutReceipt>('/api/desk/checkout', { cardNumber, barcode }),
  returnCopy: (barcode: string) => apiPost<ReturnReceipt>('/api/desk/returns', { barcode }),
  arrive: (barcode: string) => apiPost<ArrivalReceipt>('/api/desk/arrivals', { barcode }),
  renewLoan: (loanId: number) => apiPost<LoanView>(`/api/desk/loans/${loanId}/renew`),
  loans: (memberId: number) => apiGet<LoanView[]>(`/api/desk/members/${memberId}/loans`),
  history: (memberId: number) => apiGet<LoanView[]>(`/api/desk/members/${memberId}/history`),
  reservations: (memberId: number) => apiGet<ReservationView[]>(`/api/desk/members/${memberId}/reservations`),
  account: (memberId: number) => apiGet<AccountView>(`/api/desk/members/${memberId}/account`),
  reserve: (memberId: number, titleId: number, pickupBranchId: number) =>
    apiPost<ReservationView>('/api/desk/reservations', { memberId, titleId, pickupBranchId }),
  cancelReservation: (id: number) => apiPost<ReservationView>(`/api/desk/reservations/${id}/cancel`),

  pay: (memberId: number, amount: number, method: PaymentMethod) =>
    apiPost<PaymentReceipt>(`/api/desk/members/${memberId}/payments`, { amount, method }),
  waive: (chargeId: number, amount: number, reason: string) =>
    apiPost<ChargeView>(`/api/desk/charges/${chargeId}/waive`, { amount, reason }),
};
