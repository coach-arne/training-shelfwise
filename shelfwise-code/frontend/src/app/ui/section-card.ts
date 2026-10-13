import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** A titled surface that groups one task or one block of facts. Project `[actions]` for buttons next to the title. */
@Component({
  selector: 'app-section-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (title()) {
      <header>
        <div>
          <h2>{{ title() }}</h2>
          @if (subtitle()) { <p class="subtitle">{{ subtitle() }}</p> }
        </div>
        <div class="actions"><ng-content select="[actions]" /></div>
      </header>
    }
    <ng-content />
  `,
  styles: `
    :host { display: block; background: var(--sw-color-surface); border: 1px solid var(--sw-color-line); border-radius: var(--sw-radius-md);
            padding: var(--sw-space-5); margin: var(--sw-space-4) 0; box-shadow: var(--sw-shadow-card); }
    header { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--sw-space-4); margin-bottom: var(--sw-space-4); }
    h2 { margin: 0; }
    .subtitle { margin: var(--sw-space-1) 0 0; color: var(--sw-color-muted); font-size: 14px; }
    .actions { display: flex; gap: var(--sw-space-2); }
  `,
})
export class SectionCard {
  readonly title = input<string>();
  readonly subtitle = input<string>();
}
