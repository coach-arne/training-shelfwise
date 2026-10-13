// Types of the REST API (see the *Dtos classes of the backend).

export type Role = 'MEMBER' | 'LIBRARIAN' | 'BRANCH_MANAGER' | 'ADMINISTRATOR';
export type MemberCategory = 'YOUTH' | 'ADULT' | 'SENIOR';
export type MaterialType = 'BOOK' | 'AUDIOVISUAL' | 'MAGAZINE';
export type CopyStatus = 'AVAILABLE' | 'ON_LOAN' | 'ON_HOLD' | 'IN_TRANSIT' | 'LOST' | 'DAMAGED' | 'WITHDRAWN';
export type LoanStatus = 'OPEN' | 'RETURNED' | 'LOST';
export type ReservationStatus = 'WAITING' | 'ON_HOLD' | 'COLLECTED' | 'EXPIRED' | 'CANCELLED';
export type ChargeType = 'OVERDUE_FINE' | 'LOST_REPLACEMENT' | 'LOST_ADMIN_FEE' | 'RESERVATION_FEE';
export type ChargeStatus = 'OPEN' | 'SETTLED' | 'CANCELLED';
export type TransactionType = 'PAYMENT' | 'WAIVER' | 'REFUND' | 'CANCELLATION';
export type PaymentMethod = 'CASH' | 'DEBIT_CARD' | 'ONLINE';
export type PaymentSessionStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';
export type ProviderResult = 'SUCCEEDS' | 'FAILS' | 'CANCELLED';
export type RoutingOutcome = 'HOLD_HERE' | 'TRANSIT_TO_PICKUP' | 'AVAILABLE_HERE' | 'TRANSIT_TO_HOME';
export type SearchType = 'ALL' | 'TITLE' | 'AUTHOR' | 'ISBN';

export interface ApiFailure {
  status: number;
  code: string;
  ruleId: string | null;
  message: string;
  details: string[];
}

export interface UserProfile {
  role: Role;
  accountId: number;
  displayName: string;
  branchId: number | null;
  branchCode: string | null;
  memberId: number | null;
  username: string;
}

export interface LoginResult {
  token: string;
  profile: UserProfile;
}

export interface SystemInfo {
  businessDate: string;
  trainingMode: boolean;
}

export interface DemoAccount {
  type: 'STAFF' | 'MEMBER';
  id: number;
  username: string;
  displayName: string;
  role: Role;
  branchCode: string | null;
  scenario: string | null;
  description: string | null;
}

export interface DemoCopy {
  barcode: string;
  status: CopyStatus;
  currentBranchCode: string;
}

/** An example book of the seed catalogue (training aid only). */
export interface DemoBook {
  titleId: number;
  title: string;
  authors: string;
  isbn: string | null;
  materialType: MaterialType;
  copies: DemoCopy[];
}

// ----- members -----

export interface BlockReason {
  code: string;
  message: string;
}

export interface BlockStatus {
  blocked: boolean;
  membershipValid: boolean;
  balance: number;
  reasons: BlockReason[];
}

export interface GuardianInfo {
  memberId: number | null;
  name: string;
  email: string;
}

export interface MemberDetail {
  id: number;
  cardNumber: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  street: string;
  postalCode: string;
  city: string;
  email: string;
  phone: string | null;
  smsOptIn: boolean;
  keepHistory: boolean;
  category: MemberCategory;
  membershipStart: string;
  membershipEnd: string;
  guardian: GuardianInfo | null;
  status: BlockStatus;
  balance: number;
  openLoans: number;
  maxLoans: number;
  activeReservations: number;
  maxActiveReservations: number;
}

export interface MemberSummary {
  id: number;
  cardNumber: string;
  fullName: string;
  birthDate: string;
  category: MemberCategory;
  email: string;
  blocked: boolean;
}

export interface RegisterMemberRequest {
  firstName: string;
  lastName: string;
  birthDate: string;
  street: string;
  postalCode: string;
  city: string;
  email: string;
  phone: string | null;
  guardianMemberId: number | null;
  guardianName: string | null;
  guardianEmail: string | null;
  paymentMethod: PaymentMethod | null;
  confirmDuplicate: boolean;
}

export interface RegistrationResult {
  member: MemberDetail;
  feePaid: number;
  temporaryPassword: string;
}

export interface RenewalResult {
  member: MemberDetail;
  feePaid: number;
  previousCategory: MemberCategory;
}

export interface StaffUpdateMemberRequest {
  firstName: string;
  lastName: string;
  street: string;
  postalCode: string;
  city: string;
  email: string;
  phone: string | null;
  smsOptIn: boolean;
}

export interface PreferencesRequest {
  email: string;
  phone: string | null;
  smsOptIn: boolean;
  keepHistory: boolean;
}

// ----- catalogue -----

export interface BranchView {
  id: number;
  code: string;
  name: string;
}

export interface BranchAvailability {
  branchId: number;
  branchCode: string;
  branchName: string;
  available: number;
}

export interface CopyView {
  id: number;
  barcode: string;
  titleId: number;
  title: string;
  homeBranchId: number;
  homeBranchCode: string;
  currentBranchId: number;
  currentBranchCode: string;
  destinationBranchCode: string | null;
  shelfMark: string | null;
  status: CopyStatus;
  replacementCost: number | null;
  effectiveReplacementCost: number;
  acquisitionDate: string;
}

export interface TitleResult {
  id: number;
  title: string;
  authors: string;
  publisher: string | null;
  publicationYear: number | null;
  isbn: string | null;
  materialType: MaterialType;
  availability: BranchAvailability[];
  waitingReservations: number;
  copies?: CopyView[] | null;
}

export interface TitleRequest {
  title: string;
  authors: string;
  publisher: string | null;
  publicationYear: number | null;
  isbn: string | null;
  materialType: MaterialType;
}

export interface CopyRequest {
  titleId: number;
  barcode: string | null;
  homeBranchId: number;
  shelfMark: string | null;
  replacementCost: number | null;
  acquisitionDate: string | null;
}

// ----- lending -----

export interface LoanView {
  id: number;
  copyId: number;
  barcode: string;
  titleId: number;
  title: string;
  materialType: MaterialType;
  memberId: number | null;
  memberName: string | null;
  checkoutDate: string;
  dueDate: string;
  returnDate: string | null;
  status: LoanStatus;
  renewalsUsed: number;
  renewalsLeft: number;
  overdue: boolean;
  chargeableDays: number;
  accruedFine: number;
  checkoutBranchCode: string;
  canRenew: boolean;
  renewalRefusal: string | null;
}

export interface CheckoutReceipt {
  loan: LoanView;
  memberName: string;
  cardNumber: string;
  date: string;
  openLoans: LoanView[];
}

export interface RoutingInfo {
  outcome: RoutingOutcome;
  copyStatus: CopyStatus;
  destinationBranchCode: string | null;
}

export interface ReturnReceipt {
  barcode: string;
  title: string;
  returnDate: string;
  memberName: string | null;
  fineCharged: number;
  chargeableDays: number;
  routing: RoutingInfo;
  lostCopy: boolean;
  replacementCancelled: boolean;
  message: string;
}

export interface ArrivalReceipt {
  barcode: string;
  title: string;
  routing: RoutingInfo;
  message: string;
}

export interface ReservationView {
  id: number;
  titleId: number;
  title: string;
  memberId: number;
  memberName: string;
  pickupBranchId: number;
  pickupBranchCode: string;
  status: ReservationStatus;
  placedAt: string;
  queuePosition: number | null;
  holdUntil: string | null;
  notifiedOn: string | null;
  cancelledByLibrary: boolean;
  feeCharged: number | null;
}

// ----- finance -----

export interface TransactionView {
  type: TransactionType;
  amount: number;
  date: string;
  note: string | null;
}

export interface ChargeView {
  id: number;
  type: ChargeType;
  originalAmount: number;
  outstandingAmount: number;
  chargeDate: string;
  status: ChargeStatus;
  description: string;
  loanId: number | null;
  reservationId: number | null;
  history: TransactionView[];
}

export interface AccountView {
  memberId: number;
  balance: number;
  blocked: boolean;
  blockReasons: BlockReason[];
  charges: ChargeView[];
}

export interface AllocationView {
  chargeId: number;
  chargeType: ChargeType;
  amount: number;
  outstandingAfter: number;
}

export interface PaymentReceipt {
  paymentId: number;
  memberId: number;
  memberName: string;
  amount: number;
  method: PaymentMethod;
  date: string;
  allocations: AllocationView[];
  balanceAfter: number;
  blockedAfter: boolean;
}

export interface OnlinePaymentView {
  reference: string;
  amount: number;
  status: PaymentSessionStatus;
  chargeIds: number[];
  message: string | null;
}

// ----- reports, administration, training -----

export interface CirculationRow {
  branch: string;
  materialType: MaterialType;
  checkouts: number;
  returns: number;
  renewals: number;
}

export interface OverdueRow {
  loanId: number;
  memberName: string;
  cardNumber: string;
  barcode: string;
  title: string;
  materialType: MaterialType;
  dueDate: string;
  daysOverdue: number;
  chargeableDays: number;
  accruedFine: number;
  branch: string;
}

export interface FineRateView {
  id: number;
  category: MemberCategory;
  materialType: MaterialType;
  dailyRate: number;
  validFrom: string;
  editable: boolean;
}

export interface HolidayView {
  id: number;
  date: string;
  name: string;
}

export interface TrainingState {
  businessDate: string;
  lastProcessedDate: string;
}

export interface DailyProcessingSummary {
  datesProcessed: string[];
  dueReminders: number;
  overdueNotices: number;
  copiesDeclaredLost: number;
  holdsExpired: number;
  pickupReminders: number;
  membershipReminders: number;
  loansAnonymised: number;
  messagesWritten: number;
}

export interface OutboxEntry {
  id: number;
  type: string;
  channel: 'EMAIL' | 'SMS';
  recipient: string;
  memberName: string | null;
  subject: string;
  body: string;
  date: string;
  deliveryStatus: string;
  createdAt: string;
}
