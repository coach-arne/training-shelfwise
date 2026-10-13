import { CopyStatus, ReservationStatus } from '../core/models';

/** The colour family of a status pill. */
export type Tone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

export const COPY_STATUS_TONE: Record<CopyStatus, Tone> = {
  AVAILABLE: 'success',
  ON_LOAN: 'info',
  ON_HOLD: 'warning',
  IN_TRANSIT: 'warning',
  LOST: 'danger',
  DAMAGED: 'danger',
  WITHDRAWN: 'neutral',
};

export const RESERVATION_STATUS_TONE: Record<ReservationStatus, Tone> = {
  WAITING: 'info',
  ON_HOLD: 'success',
  COLLECTED: 'neutral',
  EXPIRED: 'neutral',
  CANCELLED: 'neutral',
};
