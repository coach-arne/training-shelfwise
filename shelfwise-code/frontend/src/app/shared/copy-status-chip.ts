import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { CopyStatus } from '../core/models';
import { COPY_STATUS_LABEL } from '../helpers/labels';

const COLOURS: Record<CopyStatus, string> = {
  AVAILABLE: '#2e7d32',
  ON_LOAN: '#1565c0',
  ON_HOLD: '#6a1b9a',
  IN_TRANSIT: '#ef6c00',
  LOST: '#c62828',
  DAMAGED: '#8d6e63',
  WITHDRAWN: '#546e7a',
};

@Component({
  selector: 'app-copy-status-chip',
  imports: [MatChipsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<mat-chip [style.--mat-chip-elevated-container-color]="colour()" style="--mat-chip-label-text-color: #fff">{{ label() }}</mat-chip>`,
})
export class CopyStatusChip {
  readonly status = input.required<CopyStatus>();
  protected readonly label = computed(() => COPY_STATUS_LABEL[this.status()]);
  protected readonly colour = computed(() => COLOURS[this.status()]);
}
