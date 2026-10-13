import { apiGet, apiPost, apiPut } from '../http';
import { BranchView, CopyRequest, CopyStatus, CopyView, SearchType, TitleRequest, TitleResult } from '../models';

export const catalogueApi = {
  branches: () => apiGet<BranchView[]>('/api/catalogue/branches'),
  search: (q: string, type: SearchType) => apiGet<TitleResult[]>('/api/catalogue/search', { q, type }),
  title: (id: number) => apiGet<TitleResult>(`/api/catalogue/titles/${id}`),
  createTitle: (request: TitleRequest) => apiPost<TitleResult>('/api/staff/catalogue/titles', request),
  updateTitle: (id: number, request: TitleRequest) => apiPut<TitleResult>(`/api/staff/catalogue/titles/${id}`, request),
  createCopy: (request: CopyRequest) => apiPost<CopyView>('/api/staff/catalogue/copies', request),
  updateCopy: (id: number, request: { homeBranchId: number; shelfMark: string | null; replacementCost: number | null }) =>
    apiPut<CopyView>(`/api/staff/catalogue/copies/${id}`, request),
  changeStatus: (id: number, status: CopyStatus) => apiPost<CopyView>(`/api/staff/catalogue/copies/${id}/status`, { status }),
  copyByBarcode: (barcode: string) => apiGet<CopyView>(`/api/staff/catalogue/copies/barcode/${encodeURIComponent(barcode)}`),
};
