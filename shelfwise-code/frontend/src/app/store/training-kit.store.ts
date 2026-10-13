import { signal } from '@angular/core';
import { authApi } from '../core/api/auth.api';
import { DemoAccount, DemoBook } from '../core/models';

export type TrainingKitTab = 'accounts' | 'books';

const isOpen = signal(false);
const activeTab = signal<TrainingKitTab>('accounts');
const accountList = signal<DemoAccount[]>([]);
const bookList = signal<DemoBook[]>([]);
const picked = signal<string | null>(null);

export const trainingKitOpen = isOpen.asReadonly();
export const trainingKitTab = activeTab.asReadonly();
export const demoAccounts = accountList.asReadonly();
export const demoBooks = bookList.asReadonly();
/** The last barcode chosen in the kit; desk screens take it over and clear it with `consumePickedBarcode`. */
export const pickedBarcode = picked.asReadonly();

export function openTrainingKit(tab?: TrainingKitTab): void {
  if (tab) {
    activeTab.set(tab);
  }
  isOpen.set(true);
  void loadTrainingKit();
}

export function closeTrainingKit(): void {
  isOpen.set(false);
}

export function toggleTrainingKit(): void {
  if (isOpen()) {
    closeTrainingKit();
  } else {
    openTrainingKit();
  }
}

export function selectTrainingKitTab(tab: TrainingKitTab): void {
  activeTab.set(tab);
}

export function pickBarcode(barcode: string): void {
  picked.set(barcode);
}

export function consumePickedBarcode(): void {
  picked.set(null);
}

/** The accounts are loaded once; the books are reloaded on every opening because copy statuses change while training. */
async function loadTrainingKit(): Promise<void> {
  try {
    if (accountList().length === 0) {
      accountList.set(await authApi.demoAccounts());
    }
    bookList.set(await authApi.demoBooks());
  } catch {
    // the kit simply shows empty lists when the training endpoints cannot be reached
  }
}
