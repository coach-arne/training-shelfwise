import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { TitleResult } from '../core/models';
import { MATERIAL_LABEL } from '../helpers/labels';

/** A catalogue result with availability per branch. Actions (reserve, add copy…) are projected into the card. */
@Component({
  selector: 'app-title-result-card',
  imports: [MatCardModule, MatChipsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-card class="title-card" [attr.data-testid]="'title-' + title().id">
      <mat-card-header>
        <mat-card-title>{{ title().title }}</mat-card-title>
        <mat-card-subtitle>
          {{ title().authors }} · {{ material() }}@if (title().publicationYear) { · {{ title().publicationYear }} }
          @if (title().isbn) { · ISBN {{ title().isbn }} }
        </mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <div class="availability">
          @for (branch of title().availability; track branch.branchId) {
            <span class="branch" [class.has-copy]="branch.available > 0" [attr.data-testid]="'availability-' + branch.branchCode">
              <strong>{{ branch.branchCode }}</strong> {{ branch.available }}
            </span>
          }
        </div>
        @if (title().waitingReservations > 0) {
          <small>{{ title().waitingReservations }} waiting in the reservation queue</small>
        }
        <ng-content />
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    .availability { display: flex; flex-wrap: wrap; gap: 8px; margin: 8px 0; }
    .branch { padding: 2px 10px; border-radius: 12px; background: #eceff1; color: #546e7a; font-size: 13px; }
    .branch.has-copy { background: #e3f2e5; color: #1b5e20; }
  `,
})
export class TitleResultCard {
  readonly title = input.required<TitleResult>();
  protected material(): string {
    return MATERIAL_LABEL[this.title().materialType];
  }
}
