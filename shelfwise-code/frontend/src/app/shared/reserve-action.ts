import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { BranchView } from '../core/models';

export interface ReserveRequest {
  titleId: number;
  pickupBranchId: number;
}

/** Choose a pickup branch and reserve a title (F-RES-01). */
@Component({
  selector: 'app-reserve-action',
  imports: [MatButtonModule, MatFormFieldModule, MatSelectModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="reserve">
      <mat-form-field subscriptSizing="dynamic">
        <mat-label>Pick up at</mat-label>
        <mat-select [value]="branchId()" (valueChange)="branchId.set($event)" [attr.data-testid]="'pickup-' + titleId()">
          @for (branch of branches(); track branch.id) {
            <mat-option [value]="branch.id">{{ branch.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <button mat-flat-button [disabled]="busy() || branchId() === null" (click)="submit()" [attr.data-testid]="'reserve-' + titleId()">
        {{ label() }}
      </button>
    </div>
  `,
  styles: `.reserve { display: flex; gap: 12px; align-items: center; margin-top: 8px; }`,
})
export class ReserveAction {
  readonly titleId = input.required<number>();
  readonly branches = input.required<BranchView[]>();
  readonly label = input('Reserve');
  readonly busy = input(false);
  readonly reserve = output<ReserveRequest>();

  protected readonly branchId = signal<number | null>(null);

  protected submit(): void {
    const branch = this.branchId() ?? this.branches()[0]?.id;
    if (branch !== undefined) {
      this.reserve.emit({ titleId: this.titleId(), pickupBranchId: branch });
    }
  }
}
