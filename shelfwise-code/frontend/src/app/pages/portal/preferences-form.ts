import { ChangeDetectionStrategy, Component, effect, inject, input } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { portalApi } from '../../core/api/portal.api';
import { MemberDetail } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { notifySuccess } from '../../store/notify.store';
import { refreshPortal } from '../../store/portal.store';
import { ErrorMessage } from '../../shared/error-message';

/** E-mail, phone, SMS opt-in and "keep my reading history" (F-MEM-07, F-MEM-08). */
@Component({
  selector: 'app-preferences-form',
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, ErrorMessage],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" (ngSubmit)="save()" class="form">
      <mat-form-field>
        <mat-label>E-mail address</mat-label>
        <input matInput formControlName="email" type="email" data-testid="pref-email" />
      </mat-form-field>
      <mat-form-field>
        <mat-label>Mobile phone</mat-label>
        <input matInput formControlName="phone" data-testid="pref-phone" />
      </mat-form-field>
      <mat-checkbox formControlName="smsOptIn" data-testid="pref-sms">Send me text messages (reminders and reservations)</mat-checkbox>
      <mat-checkbox formControlName="keepHistory" data-testid="pref-history">Keep my reading history</mat-checkbox>
      <app-error-message [error]="saver.error()" />
      <div><button mat-flat-button type="submit" [disabled]="form.invalid || saver.loading()" data-testid="pref-save">Save</button></div>
    </form>
  `,
  styles: `.form { display: flex; flex-direction: column; gap: 8px; max-width: 420px; }`,
})
export class PreferencesForm {
  private readonly fb = inject(FormBuilder).nonNullable;
  readonly member = input.required<MemberDetail>();

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    smsOptIn: [false],
    keepHistory: [false],
  });

  protected readonly saver = createAction(async () => {
    const value = this.form.getRawValue();
    await portalApi.updatePreferences({ ...value, phone: value.phone.trim() || null });
    await refreshPortal();
    notifySuccess('Your preferences were saved.');
  });

  constructor() {
    effect(() => {
      const member = this.member();
      this.form.reset({
        email: member.email,
        phone: member.phone ?? '',
        smsOptIn: member.smsOptIn,
        keepHistory: member.keepHistory,
      });
    });
  }

  protected save(): void {
    void this.saver.run();
  }
}
