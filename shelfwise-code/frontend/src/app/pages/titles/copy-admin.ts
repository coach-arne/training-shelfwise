import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { catalogueApi } from '../../core/api/catalogue.api';
import { BranchView, CopyStatus, CopyView, TitleResult } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { notifySuccess } from '../../store/notify.store';
import { CopyStatusChip } from '../../shared/copy-status-chip';
import { ErrorMessage } from '../../shared/error-message';
import { MoneyPipe } from '../../shared/money.pipe';
import { useConfirm } from '../../shared/confirm-dialog';

/** The copies of a title: add a copy, mark damaged / repaired / withdrawn (BR-CAT-01). */
@Component({
  selector: 'app-copy-admin',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, CopyStatusChip, ErrorMessage, MoneyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-error-message [error]="changer.error()" />
    <table class="data-table" [attr.data-testid]="'copies-' + title().id">
      <thead><tr><th>Barcode</th><th>Home</th><th>Now at</th><th>Status</th><th class="num">Replacement cost</th><th>Change status</th></tr></thead>
      <tbody>
        @for (copy of title().copies ?? []; track copy.id) {
          <tr>
            <td>{{ copy.barcode }}</td>
            <td>{{ copy.homeBranchCode }}</td>
            <td>{{ copy.currentBranchCode }}@if (copy.destinationBranchCode) { → {{ copy.destinationBranchCode }} }</td>
            <td><app-copy-status-chip [status]="copy.status" /></td>
            <td class="num">{{ copy.effectiveReplacementCost | money }}</td>
            <td>
              @if (copy.status === 'AVAILABLE') {
                <button mat-stroked-button (click)="change(copy, 'DAMAGED')" [attr.data-testid]="'damage-' + copy.barcode">Damaged</button>
                <button mat-stroked-button (click)="change(copy, 'WITHDRAWN')" [attr.data-testid]="'withdraw-' + copy.barcode">Withdraw</button>
              } @else if (copy.status === 'DAMAGED') {
                <button mat-stroked-button (click)="change(copy, 'AVAILABLE')" [attr.data-testid]="'repair-' + copy.barcode">Repaired</button>
                <button mat-stroked-button (click)="change(copy, 'WITHDRAWN')">Withdraw</button>
              }
            </td>
          </tr>
        }
      </tbody>
    </table>

    <h3>Add a copy</h3>
    <form [formGroup]="form" (ngSubmit)="add()" class="row">
      <mat-form-field>
        <mat-label>Barcode (empty: generated)</mat-label>
        <input matInput formControlName="barcode" [attr.data-testid]="'copy-barcode-' + title().id" />
      </mat-form-field>
      <mat-form-field>
        <mat-label>Home branch</mat-label>
        <mat-select formControlName="homeBranchId" [attr.data-testid]="'copy-branch-' + title().id">
          @for (branch of branches(); track branch.id) {
            <mat-option [value]="branch.id">{{ branch.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field><mat-label>Shelf mark</mat-label><input matInput formControlName="shelfMark" /></mat-form-field>
      <mat-form-field>
        <mat-label>Replacement cost (optional)</mat-label>
        <input matInput type="number" step="0.01" formControlName="replacementCost" />
      </mat-form-field>
      <button mat-flat-button type="submit" [disabled]="adder.loading() || form.invalid" [attr.data-testid]="'copy-add-' + title().id">Add copy</button>
    </form>
    <app-error-message [error]="adder.error()" />
  `,
})
export class CopyAdmin {
  private readonly fb = inject(FormBuilder).nonNullable;
  private readonly confirm = useConfirm();

  readonly title = input.required<TitleResult>();
  readonly branches = input.required<BranchView[]>();
  readonly changed = output<void>();

  protected readonly busy = signal(false);

  protected readonly form = this.fb.group({
    barcode: [''],
    homeBranchId: [null as number | null],
    shelfMark: [''],
    replacementCost: [null as number | null],
  });

  protected readonly adder = createAction(async () => {
    const value = this.form.getRawValue();
    const branchId = value.homeBranchId ?? this.branches()[0]?.id;
    if (branchId === undefined) {
      return;
    }
    await catalogueApi.createCopy({
      titleId: this.title().id,
      barcode: value.barcode.trim() || null,
      homeBranchId: branchId,
      shelfMark: value.shelfMark.trim() || null,
      replacementCost: value.replacementCost,
      acquisitionDate: null,
    });
    this.form.reset();
    notifySuccess('Copy added.');
    this.changed.emit();
  });

  protected readonly changer = createAction(async (copy: CopyView, status: CopyStatus) => {
    if (status === 'WITHDRAWN') {
      const confirmed = await this.confirm({
        title: 'Withdraw this copy?',
        message: 'A withdrawn copy leaves the collection. If it is the last copy of the title, all waiting reservations are cancelled and their fees refunded.',
        confirmLabel: 'Withdraw',
      });
      if (!confirmed) {
        return;
      }
    }
    await catalogueApi.changeStatus(copy.id, status);
    notifySuccess('Status changed.');
    this.changed.emit();
  });

  protected add(): void {
    void this.adder.run();
  }

  protected change(copy: CopyView, status: CopyStatus): void {
    void this.changer.run(copy, status);
  }
}
