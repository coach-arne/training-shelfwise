import { signal } from '@angular/core';

export interface Toast {
  id: number;
  message: string;
  kind: 'success' | 'error';
}

const current = signal<Toast | null>(null);
let counter = 0;

export const toast = current.asReadonly();

export function notifySuccess(message: string): void {
  current.set({ id: ++counter, message, kind: 'success' });
}

export function notifyError(message: string): void {
  current.set({ id: ++counter, message, kind: 'error' });
}
