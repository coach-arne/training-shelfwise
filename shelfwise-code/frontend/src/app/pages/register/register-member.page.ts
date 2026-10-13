import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';
import { deskApi } from '../../core/api/desk.api';
import { MemberSummary, PaymentMethod, RegisterMemberRequest, RegistrationResult } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { MEMBERSHIP_FEE, categoryOn } from '../../helpers/membership';
import { CATEGORY_LABEL } from '../../helpers/labels';
import { selectMember } from '../../store/member-context.store';
import { systemInfo } from '../../store/system.store';
import { ErrorMessage } from '../../shared/error-message';
import { MoneyPipe } from '../../shared/money.pipe';
import { PageHeader } from '../../ui/page-header';

/** Register a new member at the desk (F-MEM-02): duplicate warning, guardian for a YOUTH, fee payment. */
@Component({
  selector: 'app-register-member-page',
  imports: [PageHeader, 
    DatePipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    ErrorMessage, MoneyPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <app-page-header eyebrow="Staff" title="Register a member" subtitle="Check an identity document first. The membership is valid for 12 months from today." />

      @if (result(); as result) {
        <div class="success-box" data-testid="registration-result">
          <p><strong>{{ result.member.firstName }} {{ result.member.lastName }}</strong> is registered with card number
            <strong data-testid="new-card-number">{{ result.member.cardNumber }}</strong> ({{ categoryLabel(result.member.category) }}).</p>
          <p>Fee collected: {{ result.feePaid | money }}. Valid until {{ result.member.membershipEnd | date: 'dd MMM yyyy' }}.</p>
          <p>Temporary password for the member portal: <code data-testid="temporary-password">{{ result.temporaryPassword }}</code>
            (shown once – give it to the member).</p>
          <button mat-flat-button (click)="openAtDesk(result.member.id)" data-testid="open-new-member">Open at the desk</button>
          <button mat-button (click)="again()">Register another member</button>
        </div>
      } @else {
        <form [formGroup]="form" (ngSubmit)="submit(false)" class="panel grid">
          <mat-form-field><mat-label>First name</mat-label><input matInput formControlName="firstName" data-testid="reg-first-name" /></mat-form-field>
          <mat-form-field><mat-label>Last name</mat-label><input matInput formControlName="lastName" data-testid="reg-last-name" /></mat-form-field>
          <mat-form-field><mat-label>Date of birth</mat-label><input matInput type="date" formControlName="birthDate" data-testid="reg-birth-date" /></mat-form-field>
          <mat-form-field><mat-label>E-mail</mat-label><input matInput type="email" formControlName="email" data-testid="reg-email" /></mat-form-field>
          <mat-form-field><mat-label>Street and number</mat-label><input matInput formControlName="street" data-testid="reg-street" /></mat-form-field>
          <mat-form-field><mat-label>Postal code</mat-label><input matInput formControlName="postalCode" data-testid="reg-postal-code" /></mat-form-field>
          <mat-form-field><mat-label>City</mat-label><input matInput formControlName="city" data-testid="reg-city" /></mat-form-field>
          <mat-form-field><mat-label>Mobile phone (optional)</mat-label><input matInput formControlName="phone" data-testid="reg-phone" /></mat-form-field>

          @if (category(); as category) {
            <div class="full info">
              Category: <strong data-testid="reg-category">{{ categoryLabel(category) }}</strong> · Fee: <strong>{{ fee() | money }}</strong>
            </div>
          }

          @if (category() === 'YOUTH') {
            <div class="full">
              <h3>Guardian (required for a youth member)</h3>
              <p><small>Either the card number of an existing adult or senior member, or the name and e-mail address of the guardian.</small></p>
            </div>
            <mat-form-field><mat-label>Guardian card number</mat-label><input matInput formControlName="guardianCard" data-testid="reg-guardian-card" /></mat-form-field>
            <span class="or">or</span>
            <mat-form-field><mat-label>Guardian name</mat-label><input matInput formControlName="guardianName" data-testid="reg-guardian-name" /></mat-form-field>
            <mat-form-field><mat-label>Guardian e-mail</mat-label><input matInput formControlName="guardianEmail" data-testid="reg-guardian-email" /></mat-form-field>
          }

          @if (fee() > 0) {
            <mat-form-field>
              <mat-label>Fee paid by</mat-label>
              <mat-select formControlName="paymentMethod" data-testid="reg-payment-method">
                <mat-option value="CASH">Cash</mat-option>
                <mat-option value="DEBIT_CARD">Debit card</mat-option>
              </mat-select>
            </mat-form-field>
          }

          <div class="full">
            <button mat-stroked-button type="button" (click)="checkDuplicates()" [disabled]="!canCheckDuplicates()" data-testid="reg-check-duplicates">Check for an existing member</button>
          </div>
          @if (checked() && duplicates().length === 0 && !duplicateWarning()) {
            <div class="full success-box">No member with this name and date of birth.</div>
          }
          @if (duplicates().length) {
            <div class="full warning-box" data-testid="duplicate-warning">
              <strong>A member with the same name and date of birth already exists:</strong>
              <ul>
                @for (member of duplicates(); track member.id) {
                  <li>{{ member.fullName }} – card {{ member.cardNumber }} – {{ member.email }}</li>
                }
              </ul>
              <p>Register anyway only if this is a different person.</p>
            </div>
          }
          <div class="full">
            <app-error-message [error]="registrar.error()" />
            <button mat-flat-button type="submit" [disabled]="form.invalid || registrar.loading()" data-testid="reg-submit">Register and take fee</button>
            @if (duplicateWarning()) {
              <button mat-stroked-button type="button" (click)="submit(true)" [disabled]="registrar.loading()" data-testid="reg-confirm-duplicate">Register anyway</button>
            }
          </div>
        </form>
      }
    </div>
  `,
  styles: `
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 4px 16px; align-items: center; }
    .full { grid-column: 1 / -1; }
    .info { background: var(--sw-color-info-soft); color: var(--sw-color-info); padding: var(--sw-space-2) var(--sw-space-3); border-radius: var(--sw-radius-sm); }
    .or { text-align: center; color: var(--sw-color-muted); }
  `,
})
export class RegisterMemberPage {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly router = inject(Router);

  protected readonly form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    birthDate: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    street: ['', Validators.required],
    postalCode: ['', Validators.required],
    city: ['', Validators.required],
    phone: [''],
    guardianCard: [''],
    guardianName: [''],
    guardianEmail: [''],
    paymentMethod: ['CASH' as PaymentMethod],
  });

  private readonly birthDate = toSignal(this.form.controls.birthDate.valueChanges, { initialValue: '' });

  protected readonly category = computed(() => {
    const birth = this.birthDate();
    const today = systemInfo()?.businessDate;
    if (!birth || !today) {
      return null;
    }
    const [year, month, day] = today.split('-').map(Number);
    return categoryOn(birth, new Date(year, month - 1, day));
  });
  protected readonly fee = computed(() => {
    const category = this.category();
    return category ? MEMBERSHIP_FEE[category] : 0;
  });

  protected readonly result = signal<RegistrationResult | null>(null);
  protected readonly duplicates = signal<MemberSummary[]>([]);
  protected readonly checked = signal(false);
  protected readonly duplicateWarning = signal(false);
  protected readonly categoryLabel = (category: keyof typeof CATEGORY_LABEL) => CATEGORY_LABEL[category];

  protected readonly duplicateChecker = createAction(async () => {
    const value = this.form.getRawValue();
    this.duplicates.set(await deskApi.duplicates(value.firstName.trim(), value.lastName.trim(), value.birthDate));
    this.checked.set(true);
  });

  protected readonly registrar = createAction(async (confirmDuplicate: boolean) => {
    const value = this.form.getRawValue();
    this.duplicateWarning.set(false);
    const guardian = await this.resolveGuardian(value.guardianCard);
    const request: RegisterMemberRequest = {
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      birthDate: value.birthDate,
      street: value.street.trim(),
      postalCode: value.postalCode.trim(),
      city: value.city.trim(),
      email: value.email.trim(),
      phone: value.phone.trim() || null,
      guardianMemberId: guardian,
      guardianName: guardian ? null : value.guardianName.trim() || null,
      guardianEmail: guardian ? null : value.guardianEmail.trim() || null,
      paymentMethod: this.fee() > 0 ? value.paymentMethod : null,
      confirmDuplicate,
    };
    try {
      this.result.set(await deskApi.register(request));
    } catch (error) {
      if ((error as { code?: string }).code === 'POSSIBLE_DUPLICATE') {
        this.duplicateWarning.set(true);
        await this.duplicateChecker.run();
      }
      throw error;
    }
  });

  protected canCheckDuplicates(): boolean {
    const value = this.form.getRawValue();
    return Boolean(value.firstName.trim() && value.lastName.trim() && value.birthDate);
  }

  private async resolveGuardian(card: string): Promise<number | null> {
    const number = card.trim();
    return number ? (await deskApi.memberByCard(number.toUpperCase())).id : null;
  }

  protected checkDuplicates(): void {
    void this.duplicateChecker.run();
  }

  protected submit(confirmDuplicate: boolean): void {
    void this.registrar.run(confirmDuplicate);
  }

  protected openAtDesk(memberId: number): void {
    void selectMember(memberId).then(() => this.router.navigate(['/staff/desk']));
  }

  protected again(): void {
    this.result.set(null);
    this.duplicates.set([]);
    this.checked.set(false);
    this.form.reset({ paymentMethod: 'CASH' });
  }

}
