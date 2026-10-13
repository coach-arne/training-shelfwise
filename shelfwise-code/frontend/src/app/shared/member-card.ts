import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MemberDetail } from '../core/models';
import { CATEGORY_LABEL } from '../helpers/labels';
import { BlockedBadge } from './blocked-badge';
import { MoneyPipe } from './money.pipe';
import { StatTile } from '../ui/stat-tile';

/** The member at a glance: card number, category, membership, loans, balance and block status with reasons. */
@Component({
  selector: 'app-member-card',
  imports: [DatePipe, BlockedBadge, MoneyPipe, StatTile],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="member-card" data-testid="member-card">
      <header>
        <div>
          <h2>{{ member().firstName }} {{ member().lastName }}</h2>
          <p class="meta">
            <span class="mono">{{ member().cardNumber }}</span> · {{ category() }} · born {{ member().birthDate | date: 'dd MMM yyyy' }}
          </p>
        </div>
        <app-blocked-badge [status]="member().status" />
      </header>
      <div class="facts">
        <app-stat-tile label="Membership">{{ member().membershipStart | date: 'dd MMM yyyy' }} – {{ member().membershipEnd | date: 'dd MMM yyyy' }}</app-stat-tile>
        <app-stat-tile label="Balance"><span data-testid="member-balance">{{ member().balance | money }}</span></app-stat-tile>
        <app-stat-tile label="Loans">{{ member().openLoans }} of {{ member().maxLoans }}</app-stat-tile>
        <app-stat-tile label="Reservations">{{ member().activeReservations }} of {{ member().maxActiveReservations }}</app-stat-tile>
        @if (member().guardian; as guardian) {
          <app-stat-tile label="Guardian">{{ guardian.name }} <small>{{ guardian.email }}</small></app-stat-tile>
        }
      </div>
      @for (reason of member().status.reasons; track reason.code) {
        <p class="reason" data-testid="block-reason">{{ reason.message }}</p>
      }
    </section>
  `,
  styles: `
    .member-card { background: var(--sw-color-surface); border: 1px solid var(--sw-color-line); border-radius: var(--sw-radius-md); padding: var(--sw-space-5); box-shadow: var(--sw-shadow-card); }
    header { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--sw-space-4); }
    .meta { margin: var(--sw-space-1) 0 0; color: var(--sw-color-muted); font-size: 14px; }
    .facts { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: var(--sw-space-3); margin-top: var(--sw-space-4); }
    .facts app-stat-tile { font-size: 14px; }
    .reason { color: var(--sw-color-danger); background: var(--sw-color-danger-soft); padding: var(--sw-space-2) var(--sw-space-3); border-radius: var(--sw-radius-sm); margin: var(--sw-space-3) 0 0; }
  `,
})
export class MemberCard {
  readonly member = input.required<MemberDetail>();
  protected category(): string {
    return CATEGORY_LABEL[this.member().category];
  }
}
