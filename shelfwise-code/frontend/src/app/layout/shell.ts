import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { BusinessDateBanner } from '../shared/business-date-banner';
import { isAdmin, isDesk, isLoggedIn, isMember, isStaff, logout, profile } from '../store/auth.store';
import { toast } from '../store/notify.store';
import { systemInfo } from '../store/system.store';
import { ROLE_LABEL } from '../helpers/labels';
import { clearMemberContext } from '../store/member-context.store';

/** The frame of every screen: navigation per role, the business date banner and toast messages. */
@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, BusinessDateBanner],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-toolbar color="primary" class="toolbar">
      <a routerLink="/" class="brand">Shelfwise</a>
      <nav class="nav">
        <a mat-button routerLink="/catalogue" routerLinkActive="active">Catalogue</a>
        @if (isMember()) {
          <a mat-button routerLink="/portal" routerLinkActive="active">My library</a>
        }
        @if (isDesk()) {
          <a mat-button routerLink="/staff/desk" routerLinkActive="active">Desk</a>
          <a mat-button routerLink="/staff/returns" routerLinkActive="active">Returns</a>
          <a mat-button routerLink="/staff/register" routerLinkActive="active">Register member</a>
          <a mat-button routerLink="/staff/titles" routerLinkActive="active">Titles &amp; copies</a>
        }
        @if (isStaff()) {
          <a mat-button routerLink="/staff/reports" routerLinkActive="active">Reports</a>
        }
        @if (isAdmin()) {
          <a mat-button routerLink="/staff/admin" routerLinkActive="active">Administration</a>
          @if (trainingMode()) {
            <a mat-button routerLink="/staff/training" routerLinkActive="active">Training</a>
          }
        }
      </nav>
      <span class="spacer"></span>
      @if (profile(); as user) {
        <span class="user" data-testid="current-user">{{ user.displayName }} · {{ roleLabel() }}@if (user.branchCode) { · {{ user.branchCode }} }</span>
        <button mat-button (click)="signOut()" data-testid="logout">Log out</button>
      } @else {
        <a mat-button routerLink="/login" data-testid="login-link">Log in</a>
      }
    </mat-toolbar>
    <app-business-date-banner />
    <main><ng-content /></main>
  `,
  styles: `
    .toolbar { gap: 8px; position: sticky; top: 0; z-index: 10; }
    .brand { color: inherit; text-decoration: none; font-weight: 600; font-size: 20px; margin-right: 16px; }
    .nav { display: flex; flex-wrap: wrap; }
    .nav a.active { background: rgba(255, 255, 255, 0.18); }
    .spacer { flex: 1 1 auto; }
    .user { font-size: 14px; }
  `,
})
export class Shell {
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly isMember = isMember;
  protected readonly isDesk = isDesk;
  protected readonly isStaff = isStaff;
  protected readonly isAdmin = isAdmin;
  protected readonly profile = profile;
  protected readonly isLoggedIn = isLoggedIn;
  protected readonly trainingMode = () => systemInfo()?.trainingMode ?? false;
  protected readonly roleLabel = () => (profile() ? ROLE_LABEL[profile()!.role] : '');

  constructor() {
    effect(() => {
      const message = toast();
      if (message) {
        this.snackBar.open(message.message, 'OK', {
          duration: message.kind === 'error' ? 8000 : 4000,
          panelClass: message.kind === 'error' ? 'snack-error' : 'snack-success',
        });
      }
    });
  }

  protected signOut(): void {
    clearMemberContext();
    logout();
    void this.router.navigate(['/login']);
  }
}
