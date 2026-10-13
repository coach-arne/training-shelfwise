import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** One labelled figure; the value is projected so it can be money, a date or a pill. */
@Component({
  selector: 'app-stat-tile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="label">{{ label() }}</span><span class="value"><ng-content /></span>`,
  styles: `
    :host { display: flex; flex-direction: column; gap: 2px; padding: var(--sw-space-3) var(--sw-space-4); background: var(--sw-color-sunken); border-radius: var(--sw-radius-sm); min-width: 0; }
    .label { font-size: 11px; font-weight: 600; letter-spacing: 0.06em; text-transform: uppercase; color: var(--sw-color-muted); }
    .value { font-size: 18px; font-weight: 600; font-variant-numeric: tabular-nums; overflow-wrap: anywhere; }
  `,
})
export class StatTile {
  readonly label = input.required<string>();
}
