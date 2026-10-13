import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DailyProcessingSummary } from '../../core/models';

/** What the daily processing did (F-TRN-02). */
@Component({
  selector: 'app-processing-summary',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="success-box" data-testid="processing-summary">
      <strong>Daily processing ran for {{ summary().datesProcessed.length }} day(s)</strong>
      @if (summary().datesProcessed.length) { ({{ summary().datesProcessed[0] }}@if (summary().datesProcessed.length > 1) { to {{ summary().datesProcessed[summary().datesProcessed.length - 1] }} }) }
      <ul>
        <li>{{ summary().dueReminders }} due date reminders (N-01)</li>
        <li>{{ summary().overdueNotices }} overdue notices (N-02 to N-04)</li>
        <li>{{ summary().copiesDeclaredLost }} loans declared lost (N-05)</li>
        <li>{{ summary().holdsExpired }} holds expired (N-08)</li>
        <li>{{ summary().pickupReminders }} pickup reminders (N-07)</li>
        <li>{{ summary().membershipReminders }} membership reminders (N-09)</li>
        <li>{{ summary().loansAnonymised }} loans anonymised</li>
        <li>{{ summary().messagesWritten }} messages written to the outbox</li>
      </ul>
    </div>
  `,
})
export class ProcessingSummary {
  readonly summary = input.required<DailyProcessingSummary>();
}
