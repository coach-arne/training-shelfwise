import {
  ChargeType, CopyStatus, MaterialType, MemberCategory, PaymentMethod, ReservationStatus, Role, RoutingOutcome,
  TransactionType,
} from '../core/models';

export const COPY_STATUS_LABEL: Record<CopyStatus, string> = {
  AVAILABLE: 'Available',
  ON_LOAN: 'On loan',
  ON_HOLD: 'On hold',
  IN_TRANSIT: 'In transit',
  LOST: 'Lost',
  DAMAGED: 'Damaged',
  WITHDRAWN: 'Withdrawn',
};

export const MATERIAL_LABEL: Record<MaterialType, string> = {
  BOOK: 'Book',
  AUDIOVISUAL: 'Audiovisual',
  MAGAZINE: 'Magazine',
};

export const CATEGORY_LABEL: Record<MemberCategory, string> = {
  YOUTH: 'Youth',
  ADULT: 'Adult',
  SENIOR: 'Senior',
};

export const CHARGE_TYPE_LABEL: Record<ChargeType, string> = {
  OVERDUE_FINE: 'Overdue fine',
  LOST_REPLACEMENT: 'Replacement cost (lost)',
  LOST_ADMIN_FEE: 'Administration fee (lost)',
  RESERVATION_FEE: 'Reservation fee',
};

export const TRANSACTION_LABEL: Record<TransactionType, string> = {
  PAYMENT: 'Payment',
  WAIVER: 'Waiver',
  REFUND: 'Refund',
  CANCELLATION: 'Cancelled',
};

export const RESERVATION_STATUS_LABEL: Record<ReservationStatus, string> = {
  WAITING: 'Waiting',
  ON_HOLD: 'Ready for pickup',
  COLLECTED: 'Collected',
  EXPIRED: 'Expired',
  CANCELLED: 'Cancelled',
};

export const ROLE_LABEL: Record<Role, string> = {
  MEMBER: 'Member',
  LIBRARIAN: 'Librarian',
  BRANCH_MANAGER: 'Branch manager',
  ADMINISTRATOR: 'Administrator',
};

export const PAYMENT_METHOD_LABEL: Record<PaymentMethod, string> = {
  CASH: 'Cash',
  DEBIT_CARD: 'Debit card',
  ONLINE: 'Online',
};

export const ROUTING_LABEL: Record<RoutingOutcome, string> = {
  HOLD_HERE: 'Put the copy on the hold shelf',
  TRANSIT_TO_PICKUP: 'Send to the pickup branch',
  AVAILABLE_HERE: 'Put the copy on the shelf',
  TRANSIT_TO_HOME: 'Send to the home branch',
};
