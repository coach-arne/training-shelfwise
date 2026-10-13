import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DemoAccount } from '../core/models';
import { createAction } from '../helpers/async-action';
import { ROLE_LABEL } from '../helpers/labels';
import { filterAccounts } from '../helpers/training-filter';
import { demoLogin, homeRoute, profile } from '../store/auth.store';
import { ErrorMessage } from '../shared/error-message';
import { StatusPill } from '../ui/status-pill';
import { TrainingFilter } from './training-filter';

/** The seed accounts (Appendix B.1) as compact rows. Choosing one logs in without a password (F-TRN-06). */
@Component({
  selector: 'app-demo-account-list',
  imports: [ErrorMessage, StatusPill, TrainingFilter],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-training-filter placeholder="Filter: name, S-08, NPL000008…" [value]="query()" (valueChange)="query.set($event)" />
    <app-error-message [error]="login.error()" />
    <div class="list">
      @if (staff().length) {
        <p class="group">Staff</p>
        @for (account of staff(); track account.id) {
          <button type="button" class="row-button" [class.current]="isCurrent(account)" (click)="pick(account)"
                  [attr.data-testid]="'demo-' + account.username">
            <span class="main">
              <strong>{{ account.username }}</strong>
              <small>{{ roleLabel(account) }}@if (account.branchCode) { · {{ account.branchCode }} }</small>
            </span>
          </button>
        }
      }
      @if (members().length) {
        <p class="group">Members</p>
        @for (account of members(); track account.id) {
          <button type="button" class="row-button" [class.current]="isCurrent(account)" (click)="pick(account)"
                  [attr.data-testid]="'demo-' + account.username">
            <app-status-pill tone="neutral">{{ account.scenario }}</app-status-pill>
            <span class="main">
              <strong>{{ account.displayName }} <span class="card">{{ account.username }}</span></strong>
              <small>{{ account.description }}</small>
            </span>
          </button>
        }
      }
      @if (!staff().length && !members().length) {
        <p class="none">Nothing matches.</p>
      }
    </div>
    <p class="hint">Password of every seed account: <code>shelfwise</code></p>
  `,
  styles: `
    :host { display: flex; flex-direction: column; min-height: 0; gap: var(--sw-space-2); }
    .list { overflow-y: auto; min-height: 0; margin: 0 calc(var(--sw-space-2) * -1); padding: 0 var(--sw-space-2); }
    .group { margin: var(--sw-space-3) 0 var(--sw-space-1); font-size: 11px; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; color: var(--sw-color-muted); }
    .row-button { display: flex; gap: var(--sw-space-3); align-items: flex-start; width: 100%; text-align: left; padding: var(--sw-space-2) var(--sw-space-3);
                  border: 0; border-radius: var(--sw-radius-sm); background: transparent; color: inherit; font: inherit; cursor: pointer; }
    .row-button:hover { background: var(--sw-color-sunken); }
    .row-button.current { background: var(--sw-color-accent-soft); }
    .main { display: flex; flex-direction: column; min-width: 0; }
    .main small { font-size: 12px; line-height: 1.35; }
    .card { font-family: var(--sw-font-mono); font-size: 11px; font-weight: 400; color: var(--sw-color-muted); }
    .none, .hint { margin: var(--sw-space-3) 0 0; font-size: 12px; color: var(--sw-color-muted); }
  `,
})
export class DemoAccountList {
  readonly accounts = input.required<DemoAccount[]>();
  readonly picked = output<void>();

  private readonly router = inject(Router);
  protected readonly query = signal('');
  private readonly filtered = computed(() => filterAccounts(this.accounts(), this.query()));
  protected readonly staff = computed(() => this.filtered().filter((account) => account.type === 'STAFF'));
  protected readonly members = computed(() => this.filtered().filter((account) => account.type === 'MEMBER'));
  protected readonly roleLabel = (account: DemoAccount) => ROLE_LABEL[account.role];

  protected readonly login = createAction(async (account: DemoAccount) => {
    const user = await demoLogin(account.type, account.id);
    await this.router.navigateByUrl(homeRoute(user.role));
    this.picked.emit();
  });

  protected isCurrent(account: DemoAccount): boolean {
    return profile()?.username === account.username;
  }

  protected pick(account: DemoAccount): void {
    void this.login.run(account);
  }
}
