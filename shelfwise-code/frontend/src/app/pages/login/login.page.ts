import { booleanAttribute, ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { createAction } from '../../helpers/async-action';
import { homeRoute, login } from '../../store/auth.store';
import { systemInfo } from '../../store/system.store';
import { openTrainingKit } from '../../store/training-kit.store';
import { ErrorMessage } from '../../shared/error-message';
import { Icon } from '../../ui/icon';

/** Log in with a staff user name or a member card number. In training mode a quiet link opens the training kit. */
@Component({
  selector: 'app-login-page',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, ErrorMessage, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="login">
      <aside class="brand-panel">
        <app-icon name="library" [size]="36" />
        <h1>Every book in its place.</h1>
        <p>Shelfwise keeps the lending desks, the shelves and the member portals of the Northwick libraries in step.</p>
      </aside>

      <section class="form-panel">
        <h2>Welcome back</h2>
        <p class="lead">Log in with your user name or library card number.</p>
        @if (expired()) {
          <div class="warning-box">Your session has ended. Please log in again.</div>
        }
        <form (ngSubmit)="submit()" class="login-form">
          <mat-form-field appearance="outline">
            <mat-label>User name or card number</mat-label>
            <input matInput name="username" [(ngModel)]="username" autocomplete="username" data-testid="username" required />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Password</mat-label>
            <input matInput type="password" name="password" [(ngModel)]="password" autocomplete="current-password" data-testid="password" required />
          </mat-form-field>
          <app-error-message [error]="signIn.error()" />
          <button mat-flat-button type="submit" [disabled]="signIn.loading()" data-testid="login-submit">Log in</button>
        </form>
        @if (trainingMode()) {
          <button type="button" class="training-link" (click)="openAccounts()" data-testid="open-training-accounts">
            <app-icon name="flask" [size]="16" /> Use a training account
          </button>
        }
      </section>
    </div>
  `,
  styles: `
    .login { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); max-width: 980px; margin: var(--sw-space-7) auto; min-height: 520px;
             border: 1px solid var(--sw-color-line); border-radius: var(--sw-radius-lg); overflow: hidden; background: var(--sw-color-surface); box-shadow: var(--sw-shadow-card); }
    .brand-panel { display: flex; flex-direction: column; justify-content: flex-end; gap: var(--sw-space-3); padding: var(--sw-space-6);
                   background: linear-gradient(160deg, var(--sw-color-accent) 0%, var(--sw-color-accent-strong) 100%); color: #f3f6f2; }
    .brand-panel h1 { color: #fff; font-size: 38px; }
    .brand-panel p { margin: 0; max-width: 34ch; color: rgba(255, 255, 255, 0.78); }
    .form-panel { display: flex; flex-direction: column; justify-content: center; padding: var(--sw-space-6); }
    .lead { margin: var(--sw-space-1) 0 var(--sw-space-4); color: var(--sw-color-muted); }
    .login-form { display: flex; flex-direction: column; gap: var(--sw-space-1); }
    .training-link { align-self: flex-start; display: inline-flex; align-items: center; gap: 6px; margin-top: var(--sw-space-5); padding: 0; border: 0; background: none;
                     color: var(--sw-color-muted); font: inherit; font-size: 13px; cursor: pointer; text-decoration: underline; text-underline-offset: 3px; }
    .training-link:hover { color: var(--sw-color-accent); }
    @media (max-width: 760px) {
      .login { grid-template-columns: 1fr; margin: var(--sw-space-4); }
      .brand-panel { padding: var(--sw-space-5); }
      .brand-panel h1 { font-size: 28px; }
    }
  `,
})
export class LoginPage {
  private readonly router = inject(Router);

  readonly expired = input(false, { transform: booleanAttribute });
  protected readonly trainingMode = () => systemInfo()?.trainingMode ?? false;
  protected readonly username = signal('');
  protected readonly password = signal('');

  protected readonly signIn = createAction(async () => {
    const user = await login(this.username().trim(), this.password());
    await this.router.navigateByUrl(homeRoute(user.role));
  });

  protected submit(): void {
    void this.signIn.run();
  }

  protected openAccounts(): void {
    openTrainingKit('accounts');
  }
}
