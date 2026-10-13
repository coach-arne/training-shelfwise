import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { clearMemberContext, contextLoading, contextLoans, contextMember, contextReservations } from '../../store/member-context.store';
import { MemberCard } from '../../shared/member-card';
import { EmptyState } from '../../ui/empty-state';
import { PageHeader } from '../../ui/page-header';
import { SectionCard } from '../../ui/section-card';
import { AccountPanel } from './account-panel';
import { CheckoutPanel } from './checkout-panel';
import { HistoryPanel } from './history-panel';
import { LoansPanel } from './loans-panel';
import { MemberLookup } from './member-lookup';
import { MembershipPanel } from './membership-panel';
import { ReservationsPanel } from './reservations-panel';

/** The librarian's desk: find a member, then check out, renew, take payments, waive, reserve and renew membership. */
@Component({
  selector: 'app-desk-page',
  imports: [
    MatButtonModule, MatTabsModule, EmptyState, MemberCard, PageHeader, SectionCard, AccountPanel, CheckoutPanel, HistoryPanel, LoansPanel, MemberLookup,
    MembershipPanel, ReservationsPanel,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <app-page-header eyebrow="Staff" title="Desk" subtitle="Find a member, then check out, renew, take payments and manage reservations.">
        @if (member()) {
          <button actions mat-stroked-button (click)="close()" data-testid="close-member">Done with this member</button>
        }
      </app-page-header>
      <app-section-card><app-member-lookup /></app-section-card>
      @if (member(); as member) {
        <app-member-card [member]="member" />
        <mat-tab-group animationDuration="0ms" class="tabs">
          <mat-tab label="Check out"><app-section-card><app-checkout-panel /></app-section-card></mat-tab>
          <mat-tab label="Loans ({{ loans().length }})"><app-section-card><app-loans-panel /></app-section-card></mat-tab>
          <mat-tab label="Charges &amp; payment"><app-section-card><app-account-panel /></app-section-card></mat-tab>
          <mat-tab label="Reservations ({{ reservations().length }})"><app-section-card><app-reservations-panel /></app-section-card></mat-tab>
          <mat-tab label="History"><app-section-card><app-history-panel /></app-section-card></mat-tab>
          <mat-tab label="Membership"><app-section-card><app-membership-panel /></app-section-card></mat-tab>
        </mat-tab-group>
      } @else if (!loading()) {
        <app-empty-state icon="users">Search for a member to start.</app-empty-state>
      }
    </div>
  `,
  styles: `.tabs { margin-top: var(--sw-space-4); }`,
})
export class DeskPage {
  protected readonly member = contextMember;
  protected readonly loans = contextLoans;
  protected readonly reservations = contextReservations;
  protected readonly loading = contextLoading;

  protected close(): void {
    clearMemberContext();
  }
}
