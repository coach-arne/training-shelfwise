import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ApiFailure } from '../core/models';
import { errorText } from '../helpers/errors';

/** Shows a refused or failed action in plain words, with the rule that caused it. */
@Component({
  selector: 'app-error-message',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (error(); as error) {
      <div class="error" role="alert" data-testid="error-message">
        <span>{{ text(error) }}</span>
        @if (error.ruleId) {
          <small class="rule">({{ error.ruleId }})</small>
        }
      </div>
    }
  `,
  styles: `
    .error { background: var(--sw-color-danger-soft); color: var(--sw-color-danger); border: 1px solid color-mix(in srgb, var(--sw-color-danger) 25%, transparent); padding: var(--sw-space-3) var(--sw-space-4); margin: var(--sw-space-3) 0; border-radius: var(--sw-radius-sm); }
    .rule { margin-left: 8px; opacity: 0.7; }
  `,
})
export class ErrorMessage {
  readonly error = input<ApiFailure | null>(null);
  protected readonly text = errorText;
}
