import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { CopyStatus } from '../core/models';
import { COPY_STATUS_LABEL } from '../helpers/labels';
import { COPY_STATUS_TONE } from '../helpers/tones';
import { StatusPill } from '../ui/status-pill';

@Component({
  selector: 'app-copy-status-chip',
  imports: [StatusPill],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<app-status-pill [tone]="tone()">{{ label() }}</app-status-pill>`,
})
export class CopyStatusChip {
  readonly status = input.required<CopyStatus>();
  protected readonly label = computed(() => COPY_STATUS_LABEL[this.status()]);
  protected readonly tone = computed(() => COPY_STATUS_TONE[this.status()]);
}
