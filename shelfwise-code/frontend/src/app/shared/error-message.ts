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
    .error { background: #fdecea; color: #7f1d1d; border-left: 4px solid #c62828; padding: 8px 12px; margin: 8px 0; border-radius: 4px; }
    .rule { margin-left: 8px; opacity: 0.7; }
  `,
})
export class ErrorMessage {
  readonly error = input<ApiFailure | null>(null);
  protected readonly text = errorText;
}
