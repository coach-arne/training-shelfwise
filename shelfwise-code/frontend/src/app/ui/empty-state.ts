import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IconName } from '../helpers/icons';
import { Icon } from './icon';

/** A friendly placeholder for lists without content. Project an action button if there is a next step. */
@Component({
  selector: 'app-empty-state',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-icon [name]="icon()" [size]="28" />
    <p><ng-content /></p>
  `,
  styles: `
    :host { display: flex; flex-direction: column; align-items: center; gap: var(--sw-space-2); padding: var(--sw-space-6) var(--sw-space-4);
            color: var(--sw-color-muted); text-align: center; border: 1px dashed var(--sw-color-line-strong); border-radius: var(--sw-radius-md); }
    p { margin: 0; }
  `,
})
export class EmptyState {
  readonly icon = input<IconName>('inbox');
}
