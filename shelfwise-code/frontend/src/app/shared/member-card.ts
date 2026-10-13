import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MemberDetail } from '../core/models';
import { CATEGORY_LABEL } from '../helpers/labels';
import { BlockedBadge } from './blocked-badge';
import { MoneyPipe } from './money.pipe';

/** The member at a glance: card number, category, membership, loans, balance and block status with reasons. */
@Component({
  selector: 'app-member-card',
  imports: [DatePipe, MatCardModule, BlockedBadge, MoneyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-card class="member-card" data-testid="member-card">
      <mat-card-header>
        <mat-card-title>{{ member().firstName }} {{ member().lastName }}</mat-card-title>
        <mat-card-subtitle>
          Card {{ member().cardNumber }} · {{ category() }} · born {{ member().birthDate | date: 'dd MMM yyyy' }}
        </mat-card-subtitle>
        <app-blocked-badge class="badge" [status]="member().status" />
      </mat-card-header>
      <mat-card-content>
        <div class="facts">
          <div><span class="label">Membership</span>{{ member().membershipStart | date: 'dd MMM yyyy' }} – {{ member().membershipEnd | date: 'dd MMM yyyy' }}</div>
          <div><span class="label">Balance</span><strong data-testid="member-balance">{{ member().balance | money }}</strong></div>
          <div><span class="label">Loans</span>{{ member().openLoans }} of {{ member().maxLoans }}</div>
          <div><span class="label">Reservations</span>{{ member().activeReservations }} of {{ member().maxActiveReservations }}</div>
          @if (member().guardian; as guardian) {
            <div><span class="label">Guardian</span>{{ guardian.name }} ({{ guardian.email }})</div>
          }
        </div>
        @for (reason of member().status.reasons; track reason.code) {
          <p class="reason" data-testid="block-reason">{{ reason.message }}</p>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    .badge { margin-left: auto; }
    .facts { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 8px 24px; margin-top: 8px; }
    .label { display: block; font-size: 12px; color: #607080; text-transform: uppercase; }
    .reason { color: #7f1d1d; background: #fdecea; padding: 6px 10px; border-radius: 4px; margin: 8px 0 0; }
  `,
})
export class MemberCard {
  readonly member = input.required<MemberDetail>();
  protected category(): string {
    return CATEGORY_LABEL[this.member().category];
  }
}
