import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { deskApi } from '../../core/api/desk.api';
import { ArrivalReceipt, ReturnReceipt } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { fillFromPickedBarcode } from '../../helpers/picked-barcode';
import { ROUTING_LABEL } from '../../helpers/labels';
import { ErrorMessage } from '../../shared/error-message';
import { MoneyPipe } from '../../shared/money.pipe';
import { SectionCard } from '../../ui/section-card';
import { PageHeader } from '../../ui/page-header';

/** Returns (F-LEN-05) and arrival check-in of transferred copies (F-LEN-06). The result says where the copy goes. */
@Component({
  selector: 'app-returns-page',
  imports: [PageHeader, SectionCard, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, ErrorMessage, MoneyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <app-page-header eyebrow="Staff" title="Returns" subtitle="Scan a barcode to return a copy or to check in an arriving copy." />
      <app-section-card title="Return a copy">
        <form class="row" (ngSubmit)="returnCopy()">
          <mat-form-field>
            <mat-label>Copy barcode</mat-label>
            <input matInput name="barcode" [value]="barcode()" (input)="barcode.set($any($event.target).value)" data-testid="return-barcode" />
          </mat-form-field>
          <button mat-flat-button type="submit" [disabled]="!barcode().trim() || returner.loading()" data-testid="return-submit">Return</button>
        </form>
        <app-error-message [error]="returner.error()" />
        @if (returned(); as receipt) {
          <div class="success-box" data-testid="return-receipt">
            <p><strong>{{ receipt.title }}</strong> ({{ receipt.barcode }}) returned
              @if (receipt.memberName) { by {{ receipt.memberName }} }.</p>
            @if (receipt.fineCharged > 0) {
              <p class="fine" data-testid="return-fine">Overdue fine charged: <strong>{{ receipt.fineCharged | money }}</strong> ({{ receipt.chargeableDays }} chargeable days).</p>
            }
            @if (receipt.lostCopy) { <p>{{ receipt.message }}</p> }
            <p class="routing" data-testid="return-routing">{{ routing(receipt.routing.outcome) }}@if (receipt.routing.destinationBranchCode) { : {{ receipt.routing.destinationBranchCode }} }</p>
          </div>
        }
      </app-section-card>

      <app-section-card title="Check in an arriving copy">
        <p><small>For copies that were sent from another branch.</small></p>
        <form class="row" (ngSubmit)="arrive()">
          <mat-form-field>
            <mat-label>Copy barcode</mat-label>
            <input matInput name="arrival" [value]="arrivalBarcode()" (input)="arrivalBarcode.set($any($event.target).value)" data-testid="arrival-barcode" />
          </mat-form-field>
          <button mat-flat-button type="submit" [disabled]="!arrivalBarcode().trim() || arriver.loading()" data-testid="arrival-submit">Check in</button>
        </form>
        <app-error-message [error]="arriver.error()" />
        @if (arrived(); as receipt) {
          <div class="success-box" data-testid="arrival-receipt">
            <strong>{{ receipt.title }}</strong> ({{ receipt.barcode }}): {{ receipt.message }}
          </div>
        }
      </app-section-card>
    </div>
  `,
  styles: `.routing { font-family: var(--sw-font-display); font-size: 20px; font-weight: 600; margin: 4px 0 0; } .fine { color: var(--sw-color-danger); }`,
})
export class ReturnsPage {
  protected readonly barcode = signal('');
  protected readonly arrivalBarcode = signal('');
  protected readonly returned = signal<ReturnReceipt | null>(null);
  protected readonly arrived = signal<ArrivalReceipt | null>(null);
  protected readonly routing = (outcome: keyof typeof ROUTING_LABEL) => ROUTING_LABEL[outcome];

  constructor() {
    fillFromPickedBarcode(this.barcode);
  }

  protected readonly returner = createAction(async () => {
    this.returned.set(null);
    this.returned.set(await deskApi.returnCopy(this.barcode().trim()));
    this.barcode.set('');
  });

  protected readonly arriver = createAction(async () => {
    this.arrived.set(null);
    this.arrived.set(await deskApi.arrive(this.arrivalBarcode().trim()));
    this.arrivalBarcode.set('');
  });

  protected returnCopy(): void {
    void this.returner.run();
  }

  protected arrive(): void {
    void this.arriver.run();
  }
}
