import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { deskApi } from '../../core/api/desk.api';
import { PaymentMethod, RenewalResult } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { CATEGORY_LABEL } from '../../helpers/labels';
import { contextMember, refreshMemberContext } from '../../store/member-context.store';
import { notifySuccess } from '../../store/notify.store';
import { ErrorMessage } from '../../shared/error-message';
import { MoneyPipe } from '../../shared/money.pipe';

/** Membership renewal (F-MEM-04) and contact details of the member. */
@Component({
  selector: 'app-membership-panel',
  imports: [
    DatePipe, ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, ErrorMessage, MoneyPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (member(); as member) {
      <h3>Renew membership</h3>
      <p>
        Valid until <strong>{{ member.membershipEnd | date: 'dd MMM yyyy' }}</strong>. The new period starts on the day after
        it ends (or today when it has expired). The category is evaluated again on the renewal date.
      </p>
      <div class="row">
        <mat-form-field>
          <mat-label>Payment method</mat-label>
          <mat-select [value]="method()" (valueChange)="method.set($event)" data-testid="renew-method">
            <mat-option value="CASH">Cash</mat-option>
            <mat-option value="DEBIT_CARD">Debit card</mat-option>
          </mat-select>
        </mat-form-field>
        <button mat-flat-button (click)="renew(member.id)" [disabled]="renewer.loading()" data-testid="renew-membership">Renew for 12 months</button>
      </div>
      <app-error-message [error]="renewer.error()" />
      @if (renewal(); as renewal) {
        <div class="success-box" data-testid="renewal-result">
          Membership renewed until {{ renewal.member.membershipEnd | date: 'dd MMM yyyy' }}. Fee paid: {{ renewal.feePaid | money }}.
          @if (renewal.previousCategory !== renewal.member.category) {
            Category changed from {{ categoryLabel(renewal.previousCategory) }} to {{ categoryLabel(renewal.member.category) }}.
          }
        </div>
      }

      <h3>Contact details</h3>
      <form [formGroup]="form" (ngSubmit)="save(member.id)" class="grid">
        <mat-form-field><mat-label>First name</mat-label><input matInput formControlName="firstName" /></mat-form-field>
        <mat-form-field><mat-label>Last name</mat-label><input matInput formControlName="lastName" /></mat-form-field>
        <mat-form-field><mat-label>Street</mat-label><input matInput formControlName="street" /></mat-form-field>
        <mat-form-field><mat-label>Postal code</mat-label><input matInput formControlName="postalCode" /></mat-form-field>
        <mat-form-field><mat-label>City</mat-label><input matInput formControlName="city" /></mat-form-field>
        <mat-form-field><mat-label>E-mail</mat-label><input matInput formControlName="email" type="email" /></mat-form-field>
        <mat-form-field><mat-label>Mobile phone</mat-label><input matInput formControlName="phone" /></mat-form-field>
        <mat-checkbox formControlName="smsOptIn">SMS opt-in</mat-checkbox>
        <div><button mat-flat-button type="submit" [disabled]="form.invalid || saver.loading()" data-testid="member-save">Save</button></div>
      </form>
      <app-error-message [error]="saver.error()" />
    }
  `,
  styles: `.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 4px 16px; align-items: center; }`,
})
export class MembershipPanel {
  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly member = contextMember;
  protected readonly method = signal<PaymentMethod>('CASH');
  protected readonly renewal = signal<RenewalResult | null>(null);
  protected readonly categoryLabel = (category: keyof typeof CATEGORY_LABEL) => CATEGORY_LABEL[category];

  protected readonly form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    street: ['', Validators.required],
    postalCode: ['', Validators.required],
    city: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    smsOptIn: [false],
  });

  protected readonly renewer = createAction(async (memberId: number) => {
    this.renewal.set(await deskApi.renewMembership(memberId, this.method()));
    await refreshMemberContext();
    notifySuccess('Membership renewed.');
  });

  protected readonly saver = createAction(async (memberId: number) => {
    const value = this.form.getRawValue();
    await deskApi.updateMember(memberId, { ...value, phone: value.phone.trim() || null });
    await refreshMemberContext();
    notifySuccess('Member details saved.');
  });

  constructor() {
    effect(() => {
      const member = this.member();
      if (member) {
        this.form.reset({
          firstName: member.firstName, lastName: member.lastName, street: member.street,
          postalCode: member.postalCode, city: member.city, email: member.email, phone: member.phone ?? '',
          smsOptIn: member.smsOptIn,
        });
      }
    });
  }

  protected renew(memberId: number): void {
    void this.renewer.run(memberId);
  }

  protected save(memberId: number): void {
    void this.saver.run(memberId);
  }
}
