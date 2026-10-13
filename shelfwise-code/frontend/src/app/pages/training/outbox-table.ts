import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { OutboxEntry } from '../../core/models';

/** The notification outbox (F-TRN-04); a row expands to show the message text. */
@Component({
  selector: 'app-outbox-table',
  imports: [DatePipe, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (entries().length === 0) {
      <p class="empty">The outbox is empty.</p>
    } @else {
      <table class="data-table" data-testid="outbox-table">
        <thead><tr><th>Date</th><th>Type</th><th>Channel</th><th>Recipient</th><th>Member</th><th>Status</th><th></th></tr></thead>
        <tbody>
          @for (entry of entries(); track entry.id) {
            <tr>
              <td>{{ entry.date | date: 'dd MMM yyyy' }}</td>
              <td>{{ entry.type }}</td>
              <td>{{ entry.channel }}</td>
              <td>{{ entry.recipient }}</td>
              <td>{{ entry.memberName }}</td>
              <td>{{ entry.deliveryStatus }}</td>
              <td><button mat-button (click)="toggle(entry.id)">{{ openId() === entry.id ? 'Hide' : 'Show' }}</button></td>
            </tr>
            @if (openId() === entry.id) {
              <tr><td colspan="7"><strong>{{ entry.subject }}</strong><pre class="body">{{ entry.body }}</pre></td></tr>
            }
          }
        </tbody>
      </table>
    }
  `,
  styles: `.body { white-space: pre-wrap; font-family: inherit; background: #f5f7fa; padding: 8px; border-radius: 4px; }`,
})
export class OutboxTable {
  readonly entries = input.required<OutboxEntry[]>();
  protected readonly openId = signal<number | null>(null);

  protected toggle(id: number): void {
    this.openId.update((current) => (current === id ? null : id));
  }
}
