import { ChangeDetectionStrategy, Component } from '@angular/core';
import {
  closeTrainingKit, demoAccounts, demoBooks, selectTrainingKitTab, toggleTrainingKit, TrainingKitTab, trainingKitOpen,
  trainingKitTab,
} from '../store/training-kit.store';
import { Icon } from '../ui/icon';
import { DemoAccountList } from './demo-account-list';
import { DemoBookList } from './demo-book-list';

/**
 * Training aid, only rendered in training mode: a small floating button that opens a compact panel
 * with the seed accounts and the example books. It stays out of the way of the real screens.
 */
@Component({
  selector: 'app-training-kit',
  imports: [Icon, DemoAccountList, DemoBookList],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'close()' },
  template: `
    @if (open()) {
      <section class="sheet" aria-label="Training kit" data-testid="training-kit">
        <header>
          <h2>Training kit</h2>
          <button type="button" class="icon-button" (click)="close()" aria-label="Close"><app-icon name="close" [size]="18" /></button>
        </header>
        <div class="tabs" role="tablist">
          <button type="button" role="tab" [class.active]="tab() === 'accounts'" (click)="select('accounts')" data-testid="kit-tab-accounts">Accounts</button>
          <button type="button" role="tab" [class.active]="tab() === 'books'" (click)="select('books')" data-testid="kit-tab-books">Books</button>
        </div>
        @if (tab() === 'accounts') {
          <app-demo-account-list [accounts]="accounts()" (picked)="close()" />
        } @else {
          <app-demo-book-list [books]="books()" (closed)="close()" />
        }
      </section>
    }
    <button type="button" class="fab" [class.on]="open()" (click)="toggle()" data-testid="training-kit-toggle">
      <app-icon name="flask" [size]="18" /><span>Training kit</span>
    </button>
  `,
  styles: `
    .fab { position: fixed; right: var(--sw-space-5); bottom: var(--sw-space-5); z-index: 20; display: inline-flex; align-items: center; gap: var(--sw-space-2);
           padding: 8px 16px; border: 1px solid var(--sw-color-line-strong); border-radius: var(--sw-radius-pill); background: var(--sw-color-surface);
           color: var(--sw-color-ink); font: inherit; font-size: 13px; font-weight: 500; cursor: pointer; box-shadow: var(--sw-shadow-card); opacity: 0.85; }
    .fab:hover, .fab.on { opacity: 1; border-color: var(--sw-color-accent); }
    .sheet { position: fixed; right: var(--sw-space-5); bottom: 72px; z-index: 20; display: flex; flex-direction: column; gap: var(--sw-space-3);
             width: min(400px, calc(100vw - 32px)); max-height: min(560px, calc(100vh - 120px)); padding: var(--sw-space-4);
             background: var(--sw-color-surface); border: 1px solid var(--sw-color-line); border-radius: var(--sw-radius-lg); box-shadow: var(--sw-shadow-float); }
    header { display: flex; align-items: center; justify-content: space-between; }
    h2 { font-size: 17px; }
    .icon-button { display: inline-grid; place-items: center; padding: 4px; border: 0; border-radius: 50%; background: transparent; color: var(--sw-color-muted); cursor: pointer; }
    .icon-button:hover { background: var(--sw-color-sunken); color: var(--sw-color-ink); }
    .tabs { display: flex; gap: 4px; padding: 3px; border-radius: var(--sw-radius-pill); background: var(--sw-color-sunken); }
    .tabs button { flex: 1; padding: 5px 0; border: 0; border-radius: var(--sw-radius-pill); background: transparent; color: var(--sw-color-muted); font: inherit; font-size: 13px; font-weight: 500; cursor: pointer; }
    .tabs button.active { background: var(--sw-color-surface); color: var(--sw-color-ink); box-shadow: 0 1px 2px rgba(31, 42, 36, 0.12); }
    app-demo-account-list, app-demo-book-list { flex: 1; min-height: 0; }
  `,
})
export class TrainingKit {
  protected readonly open = trainingKitOpen;
  protected readonly tab = trainingKitTab;
  protected readonly accounts = demoAccounts;
  protected readonly books = demoBooks;

  protected toggle(): void {
    toggleTrainingKit();
  }

  protected close(): void {
    closeTrainingKit();
  }

  protected select(tab: TrainingKitTab): void {
    selectTrainingKitTab(tab);
  }
}
