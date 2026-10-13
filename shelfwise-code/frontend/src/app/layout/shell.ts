import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BusinessDateChip } from '../shared/business-date-chip';
import { TrainingKit } from '../training-kit/training-kit';
import { Icon } from '../ui/icon';
import { ROLE_LABEL } from '../helpers/labels';
import { initialsOf } from '../helpers/format';
import { isAdmin, isDesk, isMember, isStaff, logout, profile } from '../store/auth.store';
import { clearMemberContext } from '../store/member-context.store';
import { toast } from '../store/notify.store';
import { systemInfo } from '../store/system.store';

/** The frame of every screen: brand, navigation per role, business date, user menu, toasts and the training kit. */
@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, MatButtonModule, MatMenuModule, BusinessDateChip, Icon, TrainingKit],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="bar">
      <div class="bar-inner">
        <a routerLink="/" class="brand"><app-icon name="book" [size]="22" /><span>Shelfwise</span></a>

        <button type="button" class="menu-toggle" (click)="menuOpen.set(!menuOpen())" aria-label="Menu">
          <app-icon [name]="menuOpen() ? 'close' : 'menu'" />
        </button>

        <nav class="nav" [class.open]="menuOpen()" (click)="menuOpen.set(false)">
          <a routerLink="/catalogue" routerLinkActive="active">Catalogue</a>
          @if (isMember()) {
            <a routerLink="/portal" routerLinkActive="active">My library</a>
          }
          @if (isDesk()) {
            <a routerLink="/staff/desk" routerLinkActive="active">Desk</a>
            <a routerLink="/staff/returns" routerLinkActive="active">Returns</a>
            <a routerLink="/staff/register" routerLinkActive="active">Register member</a>
            <a routerLink="/staff/titles" routerLinkActive="active">Titles &amp; copies</a>
          }
          @if (isStaff()) {
            <a routerLink="/staff/reports" routerLinkActive="active">Reports</a>
          }
          @if (isAdmin()) {
            <a routerLink="/staff/admin" routerLinkActive="active">Administration</a>
            @if (trainingMode()) {
              <a routerLink="/staff/training" routerLinkActive="active">Training</a>
            }
          }
        </nav>

        <div class="end">
          <app-business-date-chip />
          @if (profile(); as user) {
            <button type="button" class="user" [matMenuTriggerFor]="userMenu" data-testid="current-user">
              <span class="avatar">{{ initials() }}</span>
              <span class="name">{{ user.displayName }}</span>
              <app-icon name="chevronDown" [size]="16" />
            </button>
            <mat-menu #userMenu="matMenu" xPosition="before">
              <div class="menu-head">
                <strong>{{ user.displayName }}</strong>
                <small>{{ roleLabel() }}@if (user.branchCode) { · {{ user.branchCode }} }</small>
              </div>
              <button mat-menu-item (click)="signOut()" data-testid="logout">
                <app-icon name="logOut" [size]="18" /> <span>Log out</span>
              </button>
            </mat-menu>
          } @else {
            <a mat-flat-button routerLink="/login" data-testid="login-link">Log in</a>
          }
        </div>
      </div>
    </header>
    <main><ng-content /></main>
    @if (trainingMode()) {
      <app-training-kit />
    }
  `,
  styles: `
    .bar { position: sticky; top: 0; z-index: 10; background: color-mix(in srgb, var(--sw-color-surface) 92%, transparent);
           backdrop-filter: blur(8px); border-bottom: 1px solid var(--sw-color-line); }
    .bar-inner { max-width: var(--sw-page-width); margin: 0 auto; padding: var(--sw-space-3) var(--sw-space-5); display: flex; align-items: center; gap: var(--sw-space-5); flex-wrap: wrap; }
    .brand { display: inline-flex; align-items: center; gap: var(--sw-space-2); color: var(--sw-color-accent); text-decoration: none;
             font-family: var(--sw-font-display); font-weight: 650; font-size: 22px; letter-spacing: -0.02em; }
    .nav { display: flex; gap: 2px; flex-wrap: wrap; flex: 1 1 auto; }
    .nav a { padding: 6px 14px; border-radius: var(--sw-radius-pill); color: var(--sw-color-muted); text-decoration: none; font-size: 14px; font-weight: 500; transition: background 120ms, color 120ms; }
    .nav a:hover { background: var(--sw-color-sunken); color: var(--sw-color-ink); }
    .nav a.active { background: var(--sw-color-accent-soft); color: var(--sw-color-accent-strong); }
    .end { display: flex; align-items: center; gap: var(--sw-space-3); margin-left: auto; }
    .user { display: inline-flex; align-items: center; gap: var(--sw-space-2); padding: 3px 10px 3px 3px; border-radius: var(--sw-radius-pill);
            border: 1px solid var(--sw-color-line-strong); background: var(--sw-color-surface); color: var(--sw-color-ink); font: inherit; font-size: 14px; cursor: pointer; }
    .user:hover { border-color: var(--sw-color-accent); }
    .avatar { display: inline-grid; place-items: center; width: 28px; height: 28px; border-radius: 50%; background: var(--sw-color-accent); color: #fff; font-size: 12px; font-weight: 600; }
    .menu-head { display: flex; flex-direction: column; padding: var(--sw-space-2) var(--sw-space-4) var(--sw-space-3); border-bottom: 1px solid var(--sw-color-line); margin-bottom: var(--sw-space-1); }
    .menu-toggle { display: none; margin-left: auto; padding: 6px; border: 0; border-radius: var(--sw-radius-sm); background: transparent; color: var(--sw-color-ink); cursor: pointer; }
    @media (max-width: 900px) {
      .menu-toggle { display: inline-flex; order: 2; }
      .end { order: 3; width: 100%; justify-content: space-between; margin-left: 0; }
      .nav { display: none; order: 4; width: 100%; flex-direction: column; }
      .nav.open { display: flex; }
      .name { display: none; }
    }
  `,
})
export class Shell {
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly menuOpen = signal(false);
  protected readonly isMember = isMember;
  protected readonly isDesk = isDesk;
  protected readonly isStaff = isStaff;
  protected readonly isAdmin = isAdmin;
  protected readonly profile = profile;
  protected readonly trainingMode = () => systemInfo()?.trainingMode ?? false;
  protected readonly roleLabel = () => (profile() ? ROLE_LABEL[profile()!.role] : '');
  protected readonly initials = () => initialsOf(profile()?.displayName ?? '');

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
