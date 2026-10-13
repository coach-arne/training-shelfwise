import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ICON_PATHS, IconName } from '../helpers/icons';

/** A decorative outline icon that takes the colour of the surrounding text. */
@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"
         stroke-linejoin="round" [attr.width]="size()" [attr.height]="size()" aria-hidden="true">
      <path [attr.d]="path()" />
    </svg>
  `,
  styles: `:host { display: inline-flex; flex: none; vertical-align: middle; } svg { display: block; }`,
})
export class Icon {
  readonly name = input.required<IconName>();
  readonly size = input(20);
  protected readonly path = computed(() => ICON_PATHS[this.name()]);
}
