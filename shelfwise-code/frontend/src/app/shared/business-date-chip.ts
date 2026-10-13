import { ChangeDetectionStrategy, Component } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTooltipModule } from '@angular/material/tooltip';
import { isAdmin } from '../store/auth.store';
import { systemInfo } from '../store/system.store';
import { Icon } from '../ui/icon';

/**
 * The business date ("today" for all functions, F-TRN-01) as a small chip.
 * In training mode it carries a dot, and administrators can click it to change the date.
 */
@Component({
  selector: 'app-business-date-chip',
  imports: [DatePipe, RouterLink, MatTooltipModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (info(); as info) {
      @if (info.trainingMode && isAdmin()) {
        <a class="chip" routerLink="/staff/training" matTooltip="Training mode: click to change the business date"
           data-testid="business-date-banner">
          <app-icon name="calendar" [size]="16" />
          <span>{{ info.businessDate | date: 'EEE d MMM yyyy' }}</span>
          <span class="dot"></span>
        </a>
      } @else {
        <span class="chip static" [matTooltip]="info.trainingMode ? 'Training mode' : 'Business date'" data-testid="business-date-banner">
          <app-icon name="calendar" [size]="16" />
          <span>{{ info.businessDate | date: 'EEE d MMM yyyy' }}</span>
          @if (info.trainingMode) { <span class="dot"></span> }
        </span>
      }
    }
  `,
  styles: `
    .chip { display: inline-flex; align-items: center; gap: 6px; padding: 5px 12px; border-radius: var(--sw-radius-pill);
            border: 1px solid var(--sw-color-line-strong); color: var(--sw-color-ink); font-size: 13px; text-decoration: none; background: var(--sw-color-surface); }
    a.chip:hover { border-color: var(--sw-color-accent); }
    .dot { width: 7px; height: 7px; border-radius: 50%; background: var(--sw-color-warning-accent); }
  `,
})
export class BusinessDateChip {
  protected readonly info = systemInfo;
  protected readonly isAdmin = isAdmin;
}
