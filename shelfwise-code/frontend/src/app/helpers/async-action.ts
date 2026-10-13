import { signal } from '@angular/core';
import { ApiFailure } from '../core/models';
import { toApiFailure } from './errors';

/**
 * Wraps an async function in loading and error signals, so that every screen handles busy and failed
 * calls the same way. {@code run} returns the result, or undefined when the call failed.
 */
export function createAction<A extends unknown[], T>(fn: (...args: A) => Promise<T>) {
  const loading = signal(false);
  const error = signal<ApiFailure | null>(null);

  async function run(...args: A): Promise<T | undefined> {
    loading.set(true);
    error.set(null);
    try {
      return await fn(...args);
    } catch (e) {
      error.set(toApiFailure(e));
      return undefined;
    } finally {
      loading.set(false);
    }
  }

  return { run, loading: loading.asReadonly(), error: error.asReadonly(), clearError: () => error.set(null) };
}
