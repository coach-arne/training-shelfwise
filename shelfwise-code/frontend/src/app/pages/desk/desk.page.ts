import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { clearMemberContext, contextLoading, contextLoans, contextMember, contextReservations } from '../../store/member-context.store';
import { MemberCard } from '../../shared/member-card';
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
    MatButtonModule, MatTabsModule, MemberCard, AccountPanel, CheckoutPanel, HistoryPanel, LoansPanel, MemberLookup,
    MembershipPanel, ReservationsPanel,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <h1>Desk</h1>
      <app-member-lookup />
      @if (member(); as member) {
        <div class="row current">
          <h2>Serving {{ member.firstName }} {{ member.lastName }}</h2>
          <span class="spacer"></span>
          <button mat-button (click)="close()" data-testid="close-member">Done with this member</button>
        </div>
        <app-member-card [member]="member" />
        <mat-tab-group animationDuration="0ms" class="tabs">
          <mat-tab label="Check out"><div class="panel"><app-checkout-panel /></div></mat-tab>
          <mat-tab label="Loans ({{ loans().length }})"><div class="panel"><app-loans-panel /></div></mat-tab>
          <mat-tab label="Charges &amp; payment"><div class="panel"><app-account-panel /></div></mat-tab>
          <mat-tab label="Reservations ({{ reservations().length }})"><div class="panel"><app-reservations-panel /></div></mat-tab>
          <mat-tab label="History"><div class="panel"><app-history-panel /></div></mat-tab>
          <mat-tab label="Membership"><div class="panel"><app-membership-panel /></div></mat-tab>
        </mat-tab-group>
      } @else if (!loading()) {
        <p class="empty">Search for a member to start.</p>
      }
    </div>
  `,
  styles: `.current { margin-top: 8px; } .tabs { margin-top: 12px; }`,
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
