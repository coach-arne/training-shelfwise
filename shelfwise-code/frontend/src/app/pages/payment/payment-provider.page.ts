import { ChangeDetectionStrategy, Component, input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { providerApi } from '../../core/api/portal.api';
import { OnlinePaymentView, ProviderResult } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { ErrorMessage } from '../../shared/error-message';
import { MoneyPipe } from '../../shared/money.pipe';
import { PageHeader } from '../../ui/page-header';
import { SectionCard } from '../../ui/section-card';

/**
 * The simulated payment provider (F-TRN-05): the training stand-in for the bank's payment page.
 * The member chooses the outcome; the result is reported to Shelfwise like a real provider would.
 */
@Component({
  selector: 'app-payment-provider-page',
  imports: [MatButtonModule, RouterLink, ErrorMessage, MoneyPipe, PageHeader, SectionCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page narrow">
      <app-page-header eyebrow="Training environment" title="Simulated payment provider" subtitle="No real money is moved. Choose how the payment ends." />
      <app-section-card>
          <app-error-message [error]="loader.error() ?? reporter.error()" />
          @if (payment(); as payment) {
            <p class="amount" data-testid="provider-amount">Amount to pay: <strong>{{ payment.amount | money }}</strong></p>
            @if (payment.status === 'PENDING') {
              <div class="choices">
                <button mat-flat-button (click)="report('SUCCEEDS')" [disabled]="reporter.loading()" data-testid="provider-succeeds">Payment succeeds</button>
                <button mat-stroked-button (click)="report('FAILS')" [disabled]="reporter.loading()" data-testid="provider-fails">Payment fails</button>
                <button mat-stroked-button (click)="report('CANCELLED')" [disabled]="reporter.loading()" data-testid="provider-cancel">Cancel</button>
              </div>
            } @else {
              <p [class.success-box]="payment.status === 'SUCCEEDED'" [class.warning-box]="payment.status !== 'SUCCEEDED'" data-testid="provider-result">
                {{ payment.message }}
              </p>
              <a mat-flat-button routerLink="/portal" data-testid="back-to-portal">Back to my library</a>
            }
          }
      </app-section-card>
    </div>
  `,
  styles: `
    .narrow { max-width: 560px; }
    .amount { font-size: 20px; margin-top: 0; }
    .choices { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 16px; }
  `,
})
export class PaymentProviderPage implements OnInit {
  readonly reference = input.required<string>();
  protected readonly payment = signal<OnlinePaymentView | null>(null);

  protected readonly loader = createAction(async () => {
    this.payment.set(await providerApi.payment(this.reference()));
  });

  protected readonly reporter = createAction(async (result: ProviderResult) => {
    this.payment.set(await providerApi.report(this.reference(), result));
  });

  ngOnInit(): void {
    void this.loader.run();
  }

  protected report(result: ProviderResult): void {
    void this.reporter.run(result);
  }
}
