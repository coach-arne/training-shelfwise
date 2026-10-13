import { HttpErrorResponse } from '@angular/common/http';
import { ApiFailure } from '../core/models';

function isApiFailure(error: unknown): error is ApiFailure {
  return typeof error === 'object' && error !== null && 'code' in error && 'status' in error && 'message' in error;
}

/** Converts every failure of an API call to the same shape: status, code, rule id and a readable message. */
export function toApiFailure(error: HttpErrorResponse | ApiFailure | unknown): ApiFailure {
  if (isApiFailure(error)) {
    return error;
  }
  if (error instanceof HttpErrorResponse) {
    const body = error.error as Partial<ApiFailure> | null;
    if (error.status === 0) {
      return failure(0, 'NETWORK', 'The server cannot be reached. Try again in a moment.');
    }
    if (body && typeof body === 'object' && body.code) {
      return {
        status: error.status,
        code: body.code,
        ruleId: body.ruleId ?? null,
        message: body.message ?? 'The request failed.',
        details: body.details ?? [],
      };
    }
    const message = error.status === 401 ? 'You are not logged in.' : error.status === 403 ? 'You are not allowed to do this.' : 'The request failed.';
    return failure(error.status, 'HTTP_' + error.status, message);
  }
  return failure(-1, 'UNKNOWN', 'Something went wrong.');
}

function failure(status: number, code: string, message: string): ApiFailure {
  return { status, code, ruleId: null, message, details: [] };
}

/** The text shown to the user for a failure, including field details. */
export function errorText(error: ApiFailure | null): string {
  if (!error) {
    return '';
  }
  return error.details.length ? `${error.message} ${error.details.join('; ')}` : error.message;
}
