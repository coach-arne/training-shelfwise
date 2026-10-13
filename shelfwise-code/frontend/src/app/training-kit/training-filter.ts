import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Icon } from '../ui/icon';

/** The small search field at the top of each training kit tab. */
@Component({
  selector: 'app-training-filter',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <label>
      <app-icon name="search" [size]="16" />
      <input type="search" [value]="value()" [placeholder]="placeholder()" (input)="valueChange.emit($any($event.target).value)" />
    </label>
  `,
  styles: `
    label { display: flex; align-items: center; gap: var(--sw-space-2); padding: 6px 12px; border-radius: var(--sw-radius-pill);
            background: var(--sw-color-sunken); color: var(--sw-color-muted); }
    label:focus-within { outline: 2px solid var(--sw-color-accent); }
    input { flex: 1; min-width: 0; border: 0; outline: 0; background: transparent; font: inherit; font-size: 13px; color: var(--sw-color-ink); }
  `,
})
export class TrainingFilter {
  readonly value = input('');
  readonly placeholder = input('Filter…');
  readonly valueChange = output<string>();
}
