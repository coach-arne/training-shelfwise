import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { adminApi } from '../../core/api/admin.api';
import { FineRateView, MaterialType, MemberCategory } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { CATEGORY_LABEL, MATERIAL_LABEL } from '../../helpers/labels';
import { notifySuccess } from '../../store/notify.store';
import { systemInfo } from '../../store/system.store';
import { ErrorMessage } from '../../shared/error-message';
import { MoneyPipe } from '../../shared/money.pipe';

/** Daily fine rates with a valid-from date (F-ADM-01). SENIOR rates are derived from the ADULT rate. */
@Component({
  selector: 'app-fine-rates-panel',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, ErrorMessage, MoneyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p><small>SENIOR members pay the ADULT rate with a 50% discount. Rates in the past cannot be changed; add a new rate with a later valid-from date.</small></p>
    <app-error-message [error]="loader.error() ?? remover.error()" />
    <table class="data-table" data-testid="rates-table">
      <thead><tr><th>Category</th><th>Material</th><th class="num">Rate per day</th><th>Valid from</th><th></th></tr></thead>
      <tbody>
        @for (rate of rates(); track rate.id) {
          <tr>
            <td>{{ category(rate.category) }}</td>
            <td>{{ material(rate.materialType) }}</td>
            <td class="num">{{ rate.dailyRate | money }}</td>
            <td>{{ rate.validFrom | date: 'dd MMM yyyy' }}</td>
            <td>@if (rate.editable) { <button mat-stroked-button (click)="remove(rate)" [attr.data-testid]="'delete-rate-' + rate.id">Remove</button> }</td>
          </tr>
        }
      </tbody>
    </table>

    <h3>Add a rate</h3>
    <form [formGroup]="form" (ngSubmit)="add()" class="row">
      <mat-form-field>
        <mat-label>Category</mat-label>
        <mat-select formControlName="category" data-testid="rate-category">
          <mat-option value="ADULT">Adult</mat-option>
          <mat-option value="YOUTH">Youth</mat-option>
        </mat-select>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Material type</mat-label>
        <mat-select formControlName="materialType" data-testid="rate-material">
          <mat-option value="BOOK">Book</mat-option>
          <mat-option value="AUDIOVISUAL">Audiovisual</mat-option>
          <mat-option value="MAGAZINE">Magazine</mat-option>
        </mat-select>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Rate per day (€)</mat-label>
        <input matInput type="number" step="0.01" min="0" formControlName="dailyRate" data-testid="rate-amount" />
      </mat-form-field>
      <mat-form-field>
        <mat-label>Valid from</mat-label>
        <input matInput type="date" formControlName="validFrom" data-testid="rate-valid-from" />
      </mat-form-field>
      <button mat-flat-button type="submit" [disabled]="form.invalid || adder.loading()" data-testid="rate-add">Add rate</button>
    </form>
    <app-error-message [error]="adder.error()" />
  `,
})
export class FineRatesPanel implements OnInit {
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly rates = signal<FineRateView[]>([]);
  protected readonly category = (value: MemberCategory) => CATEGORY_LABEL[value];
  protected readonly material = (value: MaterialType) => MATERIAL_LABEL[value];

  protected readonly form = this.fb.group({
    category: ['ADULT' as MemberCategory, Validators.required],
    materialType: ['BOOK' as MaterialType, Validators.required],
    dailyRate: [0.25, [Validators.required, Validators.min(0)]],
    validFrom: [systemInfo()?.businessDate ?? '', Validators.required],
  });

  protected readonly loader = createAction(async () => {
    this.rates.set(await adminApi.fineRates());
  });

  protected readonly adder = createAction(async () => {
    await adminApi.addFineRate(this.form.getRawValue());
    notifySuccess('Rate added.');
    await this.loader.run();
  });

  protected readonly remover = createAction(async (rate: FineRateView) => {
    await adminApi.deleteFineRate(rate.id);
    notifySuccess('Rate removed.');
    await this.loader.run();
  });

  ngOnInit(): void {
    void this.loader.run();
  }

  protected add(): void {
    void this.adder.run();
  }

  protected remove(rate: FineRateView): void {
    void this.remover.run(rate);
  }
}
