import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Tone } from '../helpers/tones';

/** A small rounded label whose colour tells the state at a glance. */
@Component({
  selector: 'app-status-pill',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': '"tone-" + tone()' },
  template: `<span class="dot"></span><ng-content />`,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 6px; padding: 2px 10px 2px 8px; border-radius: var(--sw-radius-pill);
            font-size: 12px; font-weight: 550; line-height: 1.6; white-space: nowrap; }
    .dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
    :host(.tone-success) { background: var(--sw-color-success-soft); color: var(--sw-color-success); }
    :host(.tone-warning) { background: var(--sw-color-warning-soft); color: var(--sw-color-warning); }
    :host(.tone-danger) { background: var(--sw-color-danger-soft); color: var(--sw-color-danger); }
    :host(.tone-info) { background: var(--sw-color-info-soft); color: var(--sw-color-info); }
    :host(.tone-neutral) { background: var(--sw-color-neutral-soft); color: var(--sw-color-neutral); }
  `,
})
export class StatusPill {
  readonly tone = input<Tone>('neutral');
}
