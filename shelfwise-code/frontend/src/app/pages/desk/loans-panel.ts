import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { deskApi } from '../../core/api/desk.api';
import { LoanView } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { contextLoans, refreshMemberContext } from '../../store/member-context.store';
import { notifySuccess } from '../../store/notify.store';
import { ErrorMessage } from '../../shared/error-message';
import { LoanTable } from '../../shared/loan-table';

/** Open loans of the member with renewal at the desk (F-LEN-04); the same rules apply as in the portal. */
@Component({
  selector: 'app-loans-panel',
  imports: [ErrorMessage, LoanTable],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-error-message [error]="action.error()" />
    <app-loan-table [loans]="loans()" [busyLoanId]="busyLoanId()" (renew)="renew($event)" emptyText="No open loans." />
  `,
})
export class LoansPanel {
  protected readonly loans = contextLoans;
  protected readonly busyLoanId = signal<number | null>(null);

  protected readonly action = createAction(async (loan: LoanView) => {
    this.busyLoanId.set(loan.id);
    try {
      const renewed = await deskApi.renewLoan(loan.id);
      await refreshMemberContext();
      notifySuccess(`"${renewed.title}" is now due on ${renewed.dueDate}.`);
    } finally {
      this.busyLoanId.set(null);
    }
  });

  protected renew(loan: LoanView): void {
    void this.action.run(loan);
  }
}
