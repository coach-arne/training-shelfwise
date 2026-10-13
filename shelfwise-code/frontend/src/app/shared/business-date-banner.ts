import { ChangeDetectionStrategy, Component } from '@angular/core';
import { DatePipe } from '@angular/common';
import { systemInfo } from '../store/system.store';

/** Shows the business date ("today" for all functions) and warns when training mode is on. */
@Component({
  selector: 'app-business-date-banner',
  imports: [DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (info(); as info) {
      <div class="banner" [class.training]="info.trainingMode" data-testid="business-date-banner">
        <span>Business date: <strong>{{ info.businessDate | date: 'EEE d MMM yyyy' }}</strong></span>
        @if (info.trainingMode) {
          <span class="training-tag">TRAINING MODE</span>
        }
      </div>
    }
  `,
  styles: `
    .banner { display: flex; gap: 16px; align-items: center; padding: 4px 16px; font-size: 13px; background: #eef2f7; color: #2b3a4a; }
    .banner.training { background: #fff3cd; color: #664d03; }
    .training-tag { font-weight: 700; letter-spacing: 0.05em; }
  `,
})
export class BusinessDateBanner {
  protected readonly info = systemInfo;
}
