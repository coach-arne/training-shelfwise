import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { portalApi } from '../../core/api/portal.api';
import { LoanView, ReservationView } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { notifySuccess } from '../../store/notify.store';
import {
  portalAccount, portalHistory, portalLoans, portalMember, portalReservations, refreshPortal,
} from '../../store/portal.store';
import { ChargeHistoryTable } from '../../shared/charge-history-table';
import { ErrorMessage } from '../../shared/error-message';
import { LoanTable } from '../../shared/loan-table';
import { MemberCard } from '../../shared/member-card';
import { MoneyPipe } from '../../shared/money.pipe';
import { ReservationTable } from '../../shared/reservation-table';
import { PreferencesForm } from './preferences-form';
import { SectionCard } from '../../ui/section-card';
import { PageHeader } from '../../ui/page-header';

/** The member portal (F-MEM-07): loans, reservations, charges with online payment, and contact preferences. */
@Component({
  selector: 'app-portal-page',
  imports: [PageHeader, SectionCard, 
    MatButtonModule, MatTabsModule, RouterLink, ChargeHistoryTable, ErrorMessage, LoanTable, MemberCard, MoneyPipe,
    PreferencesForm, ReservationTable,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <app-page-header eyebrow="Member portal" title="My library" subtitle="Your loans, reservations and account in one place." />
      <app-error-message [error]="loader.error() ?? actions.error()" />
      @if (member(); as member) {
        <app-member-card [member]="member" />
        <mat-tab-group animationDuration="0ms">
          <mat-tab label="Loans ({{ loans().length }})">
            <app-section-card>
              <app-loan-table [loans]="loans()" [busyLoanId]="busyLoanId()" (renew)="renew($event)"
                              emptyText="You have no loans at the moment." />
            </app-section-card>
            <h2>Loan history</h2>
            <app-section-card>
              <p><small>Your history is anonymised 12 months after return unless you choose to keep it (see Profile).</small></p>
              <app-loan-table [loans]="history()" [showRenew]="false" [showReturn]="true" emptyText="No history." />
            </app-section-card>
          </mat-tab>
          <mat-tab label="Reservations ({{ reservations().length }})">
            <app-section-card>
              <app-reservation-table [reservations]="reservations()" (cancel)="cancelReservation($event)" />
              <p><a mat-stroked-button routerLink="/catalogue">Find a title to reserve</a></p>
            </app-section-card>
          </mat-tab>
          <mat-tab label="Account">
            <app-section-card>
              @if (account(); as account) {
                <p>Balance: <strong data-testid="portal-balance">{{ account.balance | money }}</strong>
                  @if (account.blocked) { <span class="blocked"> – your account is blocked</span> }
                </p>
                <app-charge-history-table [charges]="account.charges" [selectable]="true" [(selectedIds)]="selectedChargeIds" />
                <div class="row pay-row">
                  <button mat-flat-button [disabled]="selectedChargeIds().length === 0 || actions.loading()" (click)="pay(selectedChargeIds())"
                          data-testid="pay-selected">Pay selected online</button>
                  <button mat-stroked-button [disabled]="account.balance <= 0 || actions.loading()" (click)="pay([])"
                          data-testid="pay-all">Pay full balance online</button>
                </div>
              }
            </app-section-card>
          </mat-tab>
          <mat-tab label="Profile">
            <app-section-card>
              <app-preferences-form [member]="member" />
            </app-section-card>
          </mat-tab>
        </mat-tab-group>
      }
    </div>
  `,
  styles: `
    .blocked { color: var(--sw-color-danger); }
    .pay-row { margin-top: 12px; }
  `,
})
export class PortalPage implements OnInit {
  private readonly router = inject(Router);

  protected readonly member = portalMember;
  protected readonly loans = portalLoans;
  protected readonly history = portalHistory;
  protected readonly reservations = portalReservations;
  protected readonly account = portalAccount;
  protected readonly busyLoanId = signal<number | null>(null);
  protected readonly selectedChargeIds = signal<number[]>([]);

  protected readonly loader = createAction(refreshPortal);

  protected readonly actions = createAction(async (task: () => Promise<void>) => task());

  ngOnInit(): void {
    void this.loader.run();
  }

  protected renew(loan: LoanView): void {
    this.busyLoanId.set(loan.id);
    void this.actions
      .run(async () => {
        const renewed = await portalApi.renew(loan.id);
        await refreshPortal();
        notifySuccess(`"${renewed.title}" is now due on ${renewed.dueDate}.`);
      })
      .finally(() => this.busyLoanId.set(null));
  }

  protected cancelReservation(reservation: ReservationView): void {
    void this.actions.run(async () => {
      await portalApi.cancelReservation(reservation.id);
      await refreshPortal();
      notifySuccess('Reservation cancelled. The reservation fee is not refunded.');
    });
  }

  /** Starts an online payment of the selected charges (all open charges when none are given) and goes to the provider. */
  protected pay(chargeIds: number[]): void {
    void this.actions.run(async () => {
      const session = await portalApi.startPayment(chargeIds);
      await this.router.navigate(['/pay', session.reference]);
    });
  }
}
