import { ChangeDetectionStrategy, Component, effect, signal } from '@angular/core';
import { deskApi } from '../../core/api/desk.api';
import { LoanView } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { contextMember } from '../../store/member-context.store';
import { ErrorMessage } from '../../shared/error-message';
import { LoanTable } from '../../shared/loan-table';

/** The loan history of the member, as far as it has not been anonymised (F-MEM-08). */
@Component({
  selector: 'app-history-panel',
  imports: [ErrorMessage, LoanTable],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-error-message [error]="loader.error()" />
    <app-loan-table [loans]="history()" [showRenew]="false" [showReturn]="true" emptyText="No loan history." />
  `,
})
export class HistoryPanel {
  protected readonly history = signal<LoanView[]>([]);

  protected readonly loader = createAction(async (memberId: number) => {
    this.history.set(await deskApi.history(memberId));
  });

  constructor() {
    effect(() => {
      const member = contextMember();
      if (member) {
        void this.loader.run(member.id);
      }
    });
  }
}
