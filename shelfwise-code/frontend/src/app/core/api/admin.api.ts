import { apiDelete, apiDownload, apiGet, apiPost } from '../http';
import {
  CirculationRow, DailyProcessingSummary, FineRateView, HolidayView, MaterialType, MemberCategory, OutboxEntry,
  OverdueRow, TrainingState,
} from '../models';

export const reportsApi = {
  circulation: (date: string | null, branchId: number | null) =>
    apiGet<CirculationRow[]>('/api/reports/r01', { date, branchId }),
  overdue: (branchId: number | null) => apiGet<OverdueRow[]>('/api/reports/r02', { branchId }),
  downloadCirculation: (date: string | null, branchId: number | null) =>
    apiDownload('/api/reports/r01', { date, branchId, format: 'csv' }, 'R-01-daily-circulation.csv'),
  downloadOverdue: (branchId: number | null) =>
    apiDownload('/api/reports/r02', { branchId, format: 'csv' }, 'R-02-overdue-items.csv'),
};

export const adminApi = {
  fineRates: () => apiGet<FineRateView[]>('/api/admin/fine-rates'),
  addFineRate: (request: { category: MemberCategory; materialType: MaterialType; dailyRate: number; validFrom: string }) =>
    apiPost<FineRateView>('/api/admin/fine-rates', request),
  deleteFineRate: (id: number) => apiDelete(`/api/admin/fine-rates/${id}`),
  holidays: (year: number) => apiGet<HolidayView[]>('/api/admin/holidays', { year }),
  addHoliday: (date: string, name: string) => apiPost<HolidayView>('/api/admin/holidays', { date, name }),
  removeHoliday: (id: number) => apiDelete(`/api/admin/holidays/${id}`),
};

export const trainingApi = {
  state: () => apiGet<TrainingState>('/api/training/state'),
  setBusinessDate: (date: string) => apiPost<DailyProcessingSummary>('/api/training/business-date', { date }),
  runDailyProcessing: () => apiPost<DailyProcessingSummary>('/api/training/daily-processing'),
  reset: () => apiPost<TrainingState>('/api/training/reset'),
  outbox: (limit: number) => apiGet<OutboxEntry[]>('/api/training/outbox', { limit }),
};
