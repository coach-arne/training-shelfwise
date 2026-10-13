import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { FineRatesPanel } from './fine-rates-panel';
import { HolidaysPanel } from './holidays-panel';
import { SectionCard } from '../../ui/section-card';
import { PageHeader } from '../../ui/page-header';

/** Administration (section 14): fine rates and public holidays. */
@Component({
  selector: 'app-admin-page',
  imports: [PageHeader, SectionCard, MatTabsModule, FineRatesPanel, HolidaysPanel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <app-page-header eyebrow="Administrator" title="Administration" subtitle="Fine rates and public holidays." />
      <mat-tab-group animationDuration="0ms">
        <mat-tab label="Fine rates"><app-section-card><app-fine-rates-panel /></app-section-card></mat-tab>
        <mat-tab label="Public holidays"><app-section-card><app-holidays-panel /></app-section-card></mat-tab>
      </mat-tab-group>
    </div>
  `,
})
export class AdminPage {}
