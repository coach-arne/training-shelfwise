import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { LoanView } from '../core/models';
import { MoneyPipe } from './money.pipe';

/** Loans of a member: due date, renewals left, accrued fine (F-FIN-07) and a renew button with the refusal reason. */
@Component({
  selector: 'app-loan-table',
  imports: [DatePipe, MatButtonModule, MatTooltipModule, MoneyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loans().length === 0) {
      <p class="empty">{{ emptyText() }}</p>
    } @else {
      <table class="data-table" data-testid="loans-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Barcode</th>
            <th>Checked out</th>
            <th>Due</th>
            @if (showReturn()) { <th>Returned</th> }
            @if (showRenew()) { <th class="num">Renewals left</th> }
            <th class="num">Accrued fine</th>
            @if (showRenew()) { <th></th> }
          </tr>
        </thead>
        <tbody>
          @for (loan of loans(); track loan.id) {
            <tr [class.overdue]="loan.overdue">
              <td>{{ loan.title }}</td>
              <td>{{ loan.barcode }}</td>
              <td>{{ loan.checkoutDate | date: 'dd MMM yyyy' }}</td>
              <td>
                {{ loan.dueDate | date: 'dd MMM yyyy' }}
                @if (loan.overdue) { <strong class="overdue-tag">overdue</strong> }
              </td>
              @if (showReturn()) { <td>{{ loan.returnDate | date: 'dd MMM yyyy' }}</td> }
              @if (showRenew()) { <td class="num">{{ loan.renewalsLeft }}</td> }
              <td class="num">{{ loan.accruedFine | money }}</td>
              @if (showRenew()) {
                <td>
                  <span [matTooltip]="loan.renewalRefusal ?? ''">
                    <button mat-stroked-button [disabled]="!loan.canRenew || busyLoanId() === loan.id"
                            (click)="renew.emit(loan)" [attr.data-testid]="'renew-' + loan.id">Renew</button>
                  </span>
                  @if (!loan.canRenew && loan.renewalRefusal) {
                    <div><small class="refusal">{{ loan.renewalRefusal }}</small></div>
                  }
                </td>
              }
            </tr>
          }
        </tbody>
      </table>
    }
  `,
  styles: `
    .overdue-tag { color: #c62828; margin-left: 6px; font-size: 12px; text-transform: uppercase; }
    tr.overdue td { background: #fff8f7; }
    .refusal { color: #7f1d1d; }
  `,
})
export class LoanTable {
  readonly loans = input.required<LoanView[]>();
  readonly showRenew = input(true);
  readonly showReturn = input(false);
  readonly busyLoanId = input<number | null>(null);
  readonly emptyText = input('No loans.');
  readonly renew = output<LoanView>();
}
