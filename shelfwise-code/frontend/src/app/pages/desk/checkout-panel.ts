import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { deskApi } from '../../core/api/desk.api';
import { CheckoutReceipt, MemberDetail } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { contextMember, refreshMemberContext } from '../../store/member-context.store';
import { ErrorMessage } from '../../shared/error-message';

/** Check out a copy to the member at the desk (F-LEN-01): scan the barcode, see the due date and all open loans. */
@Component({
  selector: 'app-checkout-panel',
  imports: [DatePipe, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, ErrorMessage],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="row" (ngSubmit)="checkout()">
      <mat-form-field>
        <mat-label>Copy barcode</mat-label>
        <input matInput name="barcode" [ngModel]="barcode()" (ngModelChange)="barcode.set($event)" data-testid="checkout-barcode" />
      </mat-form-field>
      <button mat-flat-button type="submit" [disabled]="!barcode().trim() || action.loading()" data-testid="checkout-submit">Check out</button>
    </form>
    <app-error-message [error]="action.error()" />
    @if (receipt(); as receipt) {
      <div class="success-box" data-testid="checkout-receipt">
        <strong>{{ receipt.loan.title }}</strong> is due on <strong>{{ receipt.loan.dueDate | date: 'EEEE d MMMM yyyy' }}</strong>.
      </div>
      <h3>All open loans of {{ receipt.memberName }}</h3>
      <ul>
        @for (loan of receipt.openLoans; track loan.id) {
          <li>{{ loan.title }} – due {{ loan.dueDate | date: 'dd MMM yyyy' }}</li>
        }
      </ul>
    }
  `,
})
export class CheckoutPanel {
  protected readonly barcode = signal('');
  protected readonly receipt = signal<CheckoutReceipt | null>(null);

  protected readonly action = createAction(async (member: MemberDetail) => {
    this.receipt.set(null);
    this.receipt.set(await deskApi.checkout(member.cardNumber, this.barcode().trim()));
    this.barcode.set('');
    await refreshMemberContext();
  });

  protected checkout(): void {
    const member = contextMember();
    if (member) {
      void this.action.run(member);
    }
  }
}
