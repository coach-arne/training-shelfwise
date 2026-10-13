import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ChargeView } from '../core/models';
import { CHARGE_TYPE_LABEL, TRANSACTION_LABEL } from '../helpers/labels';
import { MoneyPipe } from './money.pipe';

/**
 * The charges of a member with the history of each (payments, waivers, refunds, cancellations).
 * Optionally selectable (online payment of selected charges) and waivable (desk).
 */
@Component({
  selector: 'app-charge-history-table',
  imports: [DatePipe, MatButtonModule, MatCheckboxModule, MoneyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (charges().length === 0) {
      <p class="empty">No charges.</p>
    } @else {
      <table class="data-table" data-testid="charges-table">
        <thead>
          <tr>
            @if (selectable()) { <th></th> }
            <th>Date</th>
            <th>Charge</th>
            <th class="num">Amount</th>
            <th class="num">Outstanding</th>
            <th>Status</th>
            <th>History</th>
            @if (waivable()) { <th></th> }
          </tr>
        </thead>
        <tbody>
          @for (charge of charges(); track charge.id) {
            <tr [class.muted]="charge.status !== 'OPEN'">
              @if (selectable()) {
                <td>
                  <mat-checkbox [disabled]="charge.status !== 'OPEN'" [checked]="isSelected(charge.id)"
                                (change)="toggle(charge.id)" [attr.data-testid]="'select-charge-' + charge.id" />
                </td>
              }
              <td>{{ charge.chargeDate | date: 'dd MMM yyyy' }}</td>
              <td>{{ typeLabel(charge) }}<br /><small>{{ charge.description }}</small></td>
              <td class="num">{{ charge.originalAmount | money }}</td>
              <td class="num">{{ charge.outstandingAmount | money }}</td>
              <td>{{ charge.status }}</td>
              <td>
                @for (tx of charge.history; track $index) {
                  <div class="tx"><small>{{ transactionLabel(tx.type) }} {{ tx.amount | money }} on {{ tx.date | date: 'dd MMM' }}@if (tx.note) { ({{ tx.note }}) }</small></div>
                }
              </td>
              @if (waivable()) {
                <td>
                  @if (charge.status === 'OPEN') {
                    <button mat-stroked-button (click)="waive.emit(charge)" [attr.data-testid]="'waive-' + charge.id">Waive</button>
                  }
                </td>
              }
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class ChargeHistoryTable {
  readonly charges = input.required<ChargeView[]>();
  readonly selectable = input(false);
  readonly waivable = input(false);
  readonly selectedIds = model<number[]>([]);
  readonly waive = output<ChargeView>();

  protected readonly selected = computed(() => new Set(this.selectedIds()));
  protected readonly typeLabel = (charge: ChargeView) => CHARGE_TYPE_LABEL[charge.type];
  protected readonly transactionLabel = (type: keyof typeof TRANSACTION_LABEL) => TRANSACTION_LABEL[type];

  protected isSelected(id: number): boolean {
    return this.selected().has(id);
  }

  protected toggle(id: number): void {
    const next = new Set(this.selected());
    if (!next.delete(id)) {
      next.add(id);
    }
    this.selectedIds.set([...next]);
  }
}
