import { booleanAttribute, ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { authApi } from '../../core/api/auth.api';
import { DemoAccount } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { ROLE_LABEL } from '../../helpers/labels';
import { demoLogin, homeRoute, login } from '../../store/auth.store';
import { systemInfo } from '../../store/system.store';
import { ErrorMessage } from '../../shared/error-message';

/** Log in with a staff user name or a member card number. In training mode the seed accounts can be picked directly. */
@Component({
  selector: 'app-login-page',
  imports: [FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, ErrorMessage],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <h1>Log in</h1>
      @if (expired()) {
        <div class="warning-box">Your session has ended. Please log in again.</div>
      }
      <mat-card class="login-card">
        <mat-card-content>
          <form (ngSubmit)="submit()" class="login-form">
            <mat-form-field>
              <mat-label>User name or card number</mat-label>
              <input matInput name="username" [(ngModel)]="username" autocomplete="username" data-testid="username" required />
            </mat-form-field>
            <mat-form-field>
              <mat-label>Password</mat-label>
              <input matInput type="password" name="password" [(ngModel)]="password" autocomplete="current-password" data-testid="password" required />
            </mat-form-field>
            <app-error-message [error]="signIn.error()" />
            <button mat-flat-button type="submit" [disabled]="signIn.loading()" data-testid="login-submit">Log in</button>
          </form>
        </mat-card-content>
      </mat-card>

      @if (trainingMode() && accounts().length) {
        <h2>Training accounts</h2>
        <p>Training mode is on: pick an account to log in without a password. The password of every seed account is <code>shelfwise</code>.</p>
        <h3>Staff</h3>
        <div class="accounts">
          @for (account of staffAccounts(); track account.id) {
            <button mat-stroked-button (click)="pick(account)" [attr.data-testid]="'demo-' + account.username">
              <strong>{{ account.username }}</strong> · {{ roleLabel(account) }}@if (account.branchCode) { · {{ account.branchCode }} }
            </button>
          }
        </div>
        <h3>Members</h3>
        <div class="accounts">
          @for (account of memberAccounts(); track account.id) {
            <button mat-stroked-button (click)="pick(account)" [attr.data-testid]="'demo-' + account.username" class="member-button">
              <strong>{{ account.scenario }} · {{ account.displayName }}</strong> ({{ account.username }})
              <small>{{ account.description }}</small>
            </button>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .login-card { max-width: 420px; }
    .login-form { display: flex; flex-direction: column; gap: 8px; }
    .accounts { display: flex; flex-wrap: wrap; gap: 8px; }
    .member-button { height: auto; padding: 8px 12px; text-align: left; line-height: 1.3; }
    .member-button small { display: block; max-width: 340px; white-space: normal; color: #607080; }
  `,
})
export class LoginPage {
  private readonly router = inject(Router);

  readonly expired = input(false, { transform: booleanAttribute });
  protected readonly trainingMode = () => systemInfo()?.trainingMode ?? false;
  protected readonly username = signal('');
  protected readonly password = signal('');
  protected readonly accounts = signal<DemoAccount[]>([]);
  protected readonly staffAccounts = () => this.accounts().filter((a) => a.type === 'STAFF');
  protected readonly memberAccounts = () => this.accounts().filter((a) => a.type === 'MEMBER');
  protected readonly roleLabel = (account: DemoAccount) => ROLE_LABEL[account.role];

  protected readonly signIn = createAction(async () => {
    const user = await login(this.username().trim(), this.password());
    await this.router.navigateByUrl(homeRoute(user.role));
  });

  private readonly demo = createAction(async (account: DemoAccount) => {
    const user = await demoLogin(account.type, account.id);
    await this.router.navigateByUrl(homeRoute(user.role));
  });

  constructor() {
    authApi.demoAccounts().then((list) => this.accounts.set(list)).catch(() => this.accounts.set([]));
  }

  protected submit(): void {
    void this.signIn.run();
  }

  protected pick(account: DemoAccount): void {
    void this.demo.run(account);
  }
}
