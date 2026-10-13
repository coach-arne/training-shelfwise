import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** The top of a page: small eyebrow, serif title, optional subtitle and actions (project `[actions]`). */
@Component({
  selector: 'app-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="text">
      @if (eyebrow()) { <p class="eyebrow">{{ eyebrow() }}</p> }
      <h1>{{ title() }}</h1>
      @if (subtitle()) { <p class="subtitle">{{ subtitle() }}</p> }
    </div>
    <div class="actions"><ng-content select="[actions]" /></div>
  `,
  styles: `
    :host { display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: space-between; gap: var(--sw-space-4); margin-bottom: var(--sw-space-5); }
    .eyebrow { margin: 0 0 var(--sw-space-1); font-size: 12px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase; color: var(--sw-color-accent); }
    .subtitle { margin: var(--sw-space-2) 0 0; color: var(--sw-color-muted); max-width: 60ch; }
    .actions { display: flex; gap: var(--sw-space-2); align-items: center; }
  `,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly eyebrow = input<string>();
  readonly subtitle = input<string>();
}
