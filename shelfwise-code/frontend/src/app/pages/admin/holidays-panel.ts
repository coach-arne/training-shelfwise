import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { adminApi } from '../../core/api/admin.api';
import { HolidayView } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { notifySuccess } from '../../store/notify.store';
import { systemInfo } from '../../store/system.store';
import { ErrorMessage } from '../../shared/error-message';

/** Public holidays per year (F-ADM-02). Holidays are closing days for all branches, like Sundays. */
@Component({
  selector: 'app-holidays-panel',
  imports: [DatePipe, FormsModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, ErrorMessage],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="row">
      <mat-form-field>
        <mat-label>Year</mat-label>
        <input matInput type="number" [ngModel]="year()" (ngModelChange)="changeYear($event)" data-testid="holiday-year" />
      </mat-form-field>
    </div>
    <app-error-message [error]="loader.error() ?? remover.error()" />
    <table class="data-table" data-testid="holidays-table">
      <thead><tr><th>Date</th><th>Holiday</th><th></th></tr></thead>
      <tbody>
        @for (holiday of holidays(); track holiday.id) {
          <tr>
            <td>{{ holiday.date | date: 'EEE dd MMM yyyy' }}</td>
            <td>{{ holiday.name }}</td>
            <td><button mat-stroked-button (click)="remove(holiday)" [attr.data-testid]="'remove-holiday-' + holiday.id">Remove</button></td>
          </tr>
        }
      </tbody>
    </table>
    <h3>Add a holiday</h3>
    <form [formGroup]="form" (ngSubmit)="add()" class="row">
      <mat-form-field><mat-label>Date</mat-label><input matInput type="date" formControlName="date" data-testid="holiday-date" /></mat-form-field>
      <mat-form-field><mat-label>Name</mat-label><input matInput formControlName="name" data-testid="holiday-name" /></mat-form-field>
      <button mat-flat-button type="submit" [disabled]="form.invalid || adder.loading()" data-testid="holiday-add">Add</button>
    </form>
    <app-error-message [error]="adder.error()" />
  `,
})
export class HolidaysPanel implements OnInit {
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly year = signal(new Date().getFullYear());
  protected readonly holidays = signal<HolidayView[]>([]);
  protected readonly form = this.fb.group({
    date: ['', Validators.required],
    name: ['', Validators.required],
  });

  protected readonly loader = createAction(async () => {
    this.holidays.set(await adminApi.holidays(this.year()));
  });

  protected readonly adder = createAction(async () => {
    const value = this.form.getRawValue();
    await adminApi.addHoliday(value.date, value.name.trim());
    this.form.reset();
    notifySuccess('Holiday added.');
    await this.loader.run();
  });

  protected readonly remover = createAction(async (holiday: HolidayView) => {
    await adminApi.removeHoliday(holiday.id);
    notifySuccess('Holiday removed.');
    await this.loader.run();
  });

  ngOnInit(): void {
    const businessYear = systemInfo()?.businessDate.substring(0, 4);
    if (businessYear) {
      this.year.set(Number(businessYear));
    }
    void this.loader.run();
  }

  protected changeYear(year: number): void {
    this.year.set(year);
    void this.loader.run();
  }

  protected add(): void {
    void this.adder.run();
  }

  protected remove(holiday: HolidayView): void {
    void this.remover.run(holiday);
  }
}
