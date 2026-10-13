import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ApiFailure } from './models';
import { toApiFailure } from '../helpers/errors';

// The HttpClient is registered once at start-up (see app.config.ts), so that stores and API
// functions can be plain functions instead of injectable classes.
let client: HttpClient | undefined;

export function registerHttpClient(httpClient: HttpClient): void {
  client = httpClient;
}

function http(): HttpClient {
  if (!client) {
    throw new Error('The HttpClient is not registered yet');
  }
  return client;
}

type Params = Record<string, string | number | boolean | null | undefined>;

function toParams(params?: Params): HttpParams {
  let result = new HttpParams();
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      result = result.set(key, String(value));
    }
  });
  return result;
}

async function call<T>(request: Promise<T>): Promise<T> {
  try {
    return await request;
  } catch (error) {
    throw toApiFailure(error as HttpErrorResponse | ApiFailure);
  }
}

export function apiGet<T>(url: string, params?: Params): Promise<T> {
  return call(firstValueFrom(http().get<T>(url, { params: toParams(params) })));
}

export function apiPost<T>(url: string, body: unknown = {}): Promise<T> {
  return call(firstValueFrom(http().post<T>(url, body)));
}

export function apiPut<T>(url: string, body: unknown = {}): Promise<T> {
  return call(firstValueFrom(http().put<T>(url, body)));
}

export function apiDelete(url: string): Promise<void> {
  return call(firstValueFrom(http().delete<void>(url)));
}

/** Downloads a file (CSV export) with the bearer token that the interceptor adds. */
export async function apiDownload(url: string, params: Params, filename: string): Promise<void> {
  const blob = await call(
    firstValueFrom(http().get(url, { params: toParams(params), responseType: 'blob' })),
  );
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
}
