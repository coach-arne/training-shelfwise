import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BlockStatus } from '../core/models';
import { StatusPill } from '../ui/status-pill';

/** Shows whether a member is blocked and why (F-MEM-06). */
@Component({
  selector: 'app-blocked-badge',
  imports: [MatTooltipModule, StatusPill],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (status().blocked) {
      <app-status-pill tone="danger" data-testid="blocked-badge" [matTooltip]="reasons()">Blocked</app-status-pill>
    } @else {
      <app-status-pill tone="success" data-testid="not-blocked-badge">Active</app-status-pill>
    }
  `,
})
export class BlockedBadge {
  readonly status = input.required<BlockStatus>();

  protected reasons(): string {
    return this.status().reasons.map((reason) => reason.message).join(' ');
  }
}
