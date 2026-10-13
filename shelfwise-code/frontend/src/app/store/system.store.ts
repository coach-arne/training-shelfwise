import { signal } from '@angular/core';
import { authApi } from '../core/api/auth.api';
import { SystemInfo } from '../core/models';

const info = signal<SystemInfo | null>(null);

export const systemInfo = info.asReadonly();

/** The business date is "today" for every function (section 5.3). */
export async function refreshSystemInfo(): Promise<void> {
  try {
    info.set(await authApi.systemInfo());
  } catch {
    // the banner simply stays hidden when the backend cannot be reached
  }
}
