import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { FineRatesPanel } from './fine-rates-panel';
import { HolidaysPanel } from './holidays-panel';

/** Administration (section 14): fine rates and public holidays. */
@Component({
  selector: 'app-admin-page',
  imports: [MatTabsModule, FineRatesPanel, HolidaysPanel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <h1>Administration</h1>
      <mat-tab-group animationDuration="0ms">
        <mat-tab label="Fine rates"><div class="panel"><app-fine-rates-panel /></div></mat-tab>
        <mat-tab label="Public holidays"><div class="panel"><app-holidays-panel /></div></mat-tab>
      </mat-tab-group>
    </div>
  `,
})
export class AdminPage {}
