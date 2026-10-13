import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TitleResult } from '../core/models';
import { MATERIAL_LABEL } from '../helpers/labels';
import { StatusPill } from '../ui/status-pill';

/** A catalogue result with availability per branch. Actions (reserve, add copy…) are projected into the card. */
@Component({
  selector: 'app-title-result-card',
  imports: [StatusPill],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="title-card" [attr.data-testid]="'title-' + title().id">
      <header>
        <h3>{{ title().title }}</h3>
        <p class="meta">
          {{ title().authors }}
          <span>{{ material() }}</span>
          @if (title().publicationYear) { <span>{{ title().publicationYear }}</span> }
          @if (title().isbn) { <span class="mono">ISBN {{ title().isbn }}</span> }
        </p>
      </header>
      <div class="availability">
        @for (branch of title().availability; track branch.branchId) {
          <app-status-pill [tone]="branch.available > 0 ? 'success' : 'neutral'" [attr.data-testid]="'availability-' + branch.branchCode">
            <strong>{{ branch.branchCode }}</strong>&nbsp;{{ branch.available }} available
          </app-status-pill>
        }
        @if (title().waitingReservations > 0) {
          <small>{{ title().waitingReservations }} waiting in the reservation queue</small>
        }
      </div>
      <ng-content />
    </article>
  `,
  styles: `
    .title-card { background: var(--sw-color-surface); border: 1px solid var(--sw-color-line); border-radius: var(--sw-radius-md);
                  padding: var(--sw-space-5); margin: var(--sw-space-3) 0; box-shadow: var(--sw-shadow-card); }
    h3 { font-size: 20px; }
    .meta { margin: var(--sw-space-1) 0 0; color: var(--sw-color-muted); font-size: 14px; }
    .meta span::before { content: '·'; margin: 0 var(--sw-space-2); }
    .availability { display: flex; flex-wrap: wrap; align-items: center; gap: var(--sw-space-2); margin: var(--sw-space-4) 0 var(--sw-space-2); }
  `,
})
export class TitleResultCard {
  readonly title = input.required<TitleResult>();
  protected material(): string {
    return MATERIAL_LABEL[this.title().materialType];
  }
}
