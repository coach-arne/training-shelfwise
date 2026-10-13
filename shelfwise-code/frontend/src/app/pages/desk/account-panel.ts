import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { deskApi } from '../../core/api/desk.api';
import { ChargeView, PaymentMethod, PaymentReceipt } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { CHARGE_TYPE_LABEL } from '../../helpers/labels';
import { canWaiveMoreThanLimit } from '../../store/auth.store';
import { contextAccount, contextMember, refreshMemberContext } from '../../store/member-context.store';
import { notifySuccess } from '../../store/notify.store';
import { ChargeHistoryTable } from '../../shared/charge-history-table';
import { ErrorMessage } from '../../shared/error-message';
import { MoneyPipe } from '../../shared/money.pipe';
import { SectionCard } from '../../ui/section-card';

/** Charges, desk payment (F-PAY-01) and waivers (F-FIN-10) of the member. */
@Component({
  selector: 'app-account-panel',
  imports: [SectionCard, 
    FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, ChargeHistoryTable,
    ErrorMessage, MoneyPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (account(); as account) {
      <p>Balance: <strong data-testid="account-balance">{{ account.balance | money }}</strong>
        @for (reason of account.blockReasons; track reason.code) {
          <span class="blocked"> · {{ reason.message }}</span>
        }
      </p>
      <app-charge-history-table [charges]="account.charges" [waivable]="true" (waive)="startWaiver($event)" />

      @if (waiving(); as charge) {
        <app-section-card data-testid="waiver-form">
          <h3>Waive part of: {{ typeLabel(charge) }} ({{ charge.outstandingAmount | money }} outstanding)</h3>
          @if (!isManager) {
            <p><small>A librarian can waive at most € 2.00 per charge in total; a branch manager can waive more.</small></p>
          }
          <div class="row">
            <mat-form-field>
              <mat-label>Amount (€)</mat-label>
              <input matInput type="number" step="0.01" min="0.01" [ngModel]="waiveAmount()" (ngModelChange)="waiveAmount.set($event)" data-testid="waive-amount" />
            </mat-form-field>
            <mat-form-field class="reason">
              <mat-label>Reason (required)</mat-label>
              <input matInput [ngModel]="waiveReason()" (ngModelChange)="waiveReason.set($event)" data-testid="waive-reason" />
            </mat-form-field>
            <button mat-flat-button (click)="waive(charge)" [disabled]="waiver.loading()" data-testid="waive-submit">Waive</button>
            <button mat-button (click)="waiving.set(null)">Cancel</button>
          </div>
          <app-error-message [error]="waiver.error()" />
        </app-section-card>
      }

      @if (account.balance > 0) {
        <h3>Take a payment</h3>
        <div class="row">
          <mat-form-field>
            <mat-label>Amount (€)</mat-label>
            <input matInput type="number" step="0.01" min="0.01" [max]="account.balance" [ngModel]="amountOrBalance(account.balance)"
                   (ngModelChange)="amount.set($event)" data-testid="payment-amount" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Method</mat-label>
            <mat-select [value]="method()" (valueChange)="method.set($event)" data-testid="payment-method">
              <mat-option value="CASH">Cash</mat-option>
              <mat-option value="DEBIT_CARD">Debit card</mat-option>
            </mat-select>
          </mat-form-field>
          <button mat-flat-button (click)="pay(account.balance)" [disabled]="payer.loading()" data-testid="payment-submit">Take payment</button>
        </div>
        <app-error-message [error]="payer.error()" />
      }

      @if (receipt(); as receipt) {
        <div class="success-box" data-testid="payment-receipt">
          Payment of <strong>{{ receipt.amount | money }}</strong> received. Balance now {{ receipt.balanceAfter | money }}@if (!receipt.blockedAfter) { – member is not blocked }.
          <ul>
            @for (allocation of receipt.allocations; track allocation.chargeId) {
              <li>{{ allocation.amount | money }} to charge {{ allocation.chargeId }} ({{ allocation.outstandingAfter | money }} left)</li>
            }
          </ul>
        </div>
      }
    }
  `,
  styles: `
    .blocked { color: var(--sw-color-danger); }
    .reason { min-width: 280px; }
  `,
})
export class AccountPanel {
  protected readonly account = contextAccount;
  protected readonly amount = signal<number | null>(null);
  protected readonly method = signal<PaymentMethod>('CASH');
  protected readonly receipt = signal<PaymentReceipt | null>(null);
  protected readonly waiving = signal<ChargeView | null>(null);
  protected readonly waiveAmount = signal<number | null>(null);
  protected readonly waiveReason = signal('');
  protected readonly isManager = canWaiveMoreThanLimit();
  protected readonly typeLabel = (charge: ChargeView) => CHARGE_TYPE_LABEL[charge.type];

  protected readonly payer = createAction(async (balance: number) => {
    const member = contextMember();
    if (!member) {
      return;
    }
    this.receipt.set(await deskApi.pay(member.id, this.amount() ?? balance, this.method()));
    this.amount.set(null);
    await refreshMemberContext();
    notifySuccess('Payment received.');
  });

  protected readonly waiver = createAction(async (charge: ChargeView) => {
    await deskApi.waive(charge.id, this.waiveAmount() ?? 0, this.waiveReason());
    this.waiving.set(null);
    await refreshMemberContext();
    notifySuccess('Waiver recorded.');
  });

  protected amountOrBalance(balance: number): number {
    return this.amount() ?? balance;
  }

  protected pay(balance: number): void {
    void this.payer.run(balance);
  }

  protected startWaiver(charge: ChargeView): void {
    this.waiver.clearError();
    this.waiveAmount.set(charge.outstandingAmount);
    this.waiveReason.set('');
    this.waiving.set(charge);
  }

  protected waive(charge: ChargeView): void {
    void this.waiver.run(charge);
  }
}
