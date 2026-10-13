import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { trainingApi } from '../../core/api/admin.api';
import { DailyProcessingSummary, OutboxEntry, TrainingState } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { notifySuccess } from '../../store/notify.store';
import { refreshSystemInfo } from '../../store/system.store';
import { ErrorMessage } from '../../shared/error-message';
import { useConfirm } from '../../shared/confirm-dialog';
import { OutboxTable } from './outbox-table';
import { ProcessingSummary } from './processing-summary';
import { SectionCard } from '../../ui/section-card';
import { PageHeader } from '../../ui/page-header';

/** Training support (section 15): business date, daily processing, reset to seed data and the notification outbox. */
@Component({
  selector: 'app-training-page',
  imports: [PageHeader, SectionCard, DatePipe, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatTabsModule, ErrorMessage, OutboxTable, ProcessingSummary],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <app-page-header eyebrow="Administrator" title="Training" subtitle="Move the business date, run the daily processing and reset the seed data." />
      <div class="warning-box">Training mode is on. These functions change the shared training data.</div>
      <app-error-message [error]="loader.error() ?? mover.error() ?? runner.error() ?? resetter.error()" />
      @if (state(); as state) {
        <app-section-card>
          <p>Business date: <strong data-testid="training-business-date">{{ state.businessDate | date: 'EEE d MMM yyyy' }}</strong>
            · Daily processing last ran for: <strong data-testid="training-last-processed">{{ state.lastProcessedDate | date: 'EEE d MMM yyyy' }}</strong></p>
          <div class="row">
            <mat-form-field>
              <mat-label>New business date</mat-label>
              <input matInput type="date" [ngModel]="target()" (ngModelChange)="target.set($event)" data-testid="training-date" />
            </mat-form-field>
            <button mat-flat-button (click)="move()" [disabled]="!target() || mover.loading()" data-testid="training-set-date">Move business date</button>
            <button mat-stroked-button (click)="run()" [disabled]="runner.loading()" data-testid="training-run">Run daily processing now</button>
            <span class="spacer"></span>
            <button mat-stroked-button (click)="reset()" [disabled]="resetter.loading()" data-testid="training-reset">Reset to seed data</button>
          </div>
          <p><small>The business date can only move forward. Moving it forward runs the daily processing of every day before it, in date order.
            To go back, reset to the seed data (Monday 16 March 2026).</small></p>
        </app-section-card>
        @if (summary(); as summary) {
          <app-processing-summary [summary]="summary" />
        }
      }
      <h2>Notification outbox</h2>
      <p><small>Nothing is really sent: every message is written here with its date, type, channel and recipient.</small></p>
      <div class="row"><button mat-stroked-button (click)="loadOutbox()" data-testid="outbox-refresh">Refresh</button></div>
      <app-outbox-table [entries]="outbox()" />
    </div>
  `,
})
export class TrainingPage implements OnInit {
  private readonly confirm = useConfirm();

  protected readonly state = signal<TrainingState | null>(null);
  protected readonly target = signal('');
  protected readonly summary = signal<DailyProcessingSummary | null>(null);
  protected readonly outbox = signal<OutboxEntry[]>([]);

  protected readonly loader = createAction(async () => {
    const [state, outbox] = await Promise.all([trainingApi.state(), trainingApi.outbox(200)]);
    this.state.set(state);
    this.outbox.set(outbox);
    if (!this.target()) {
      this.target.set(state.businessDate);
    }
  });

  protected readonly mover = createAction(async () => {
    this.summary.set(await trainingApi.setBusinessDate(this.target()));
    await this.afterChange('Business date moved.');
  });

  protected readonly runner = createAction(async () => {
    this.summary.set(await trainingApi.runDailyProcessing());
    await this.afterChange('Daily processing done.');
  });

  protected readonly resetter = createAction(async () => {
    const confirmed = await this.confirm({
      title: 'Reset to seed data?',
      message: 'All changes are lost: members, loans, charges, reservations and the outbox return to the starting situation of Monday 16 March 2026.',
      confirmLabel: 'Reset',
    });
    if (!confirmed) {
      return;
    }
    this.state.set(await trainingApi.reset());
    this.summary.set(null);
    this.target.set(this.state()?.businessDate ?? '');
    await this.afterChange('Seed data restored.');
  });

  ngOnInit(): void {
    void this.loader.run();
  }

  private async afterChange(message: string): Promise<void> {
    await refreshSystemInfo();
    await this.loader.run();
    notifySuccess(message);
  }

  protected move(): void {
    void this.mover.run();
  }

  protected run(): void {
    void this.runner.run();
  }

  protected reset(): void {
    void this.resetter.run();
  }

  protected loadOutbox(): void {
    void this.loader.run();
  }
}
