import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BlockStatus } from '../core/models';

/** Shows whether a member is blocked and why (F-MEM-06). */
@Component({
  selector: 'app-blocked-badge',
  imports: [MatChipsModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (status().blocked) {
      <mat-chip class="blocked" data-testid="blocked-badge" [matTooltip]="reasons()">Blocked</mat-chip>
    } @else {
      <mat-chip class="ok" data-testid="not-blocked-badge">Active</mat-chip>
    }
  `,
  styles: `
    .blocked { --mat-chip-label-text-color: #fff; --mat-chip-elevated-container-color: #c62828; }
    .ok { --mat-chip-label-text-color: #fff; --mat-chip-elevated-container-color: #2e7d32; }
  `,
})
export class BlockedBadge {
  readonly status = input.required<BlockStatus>();

  protected reasons(): string {
    return this.status().reasons.map((reason) => reason.message).join(' ');
  }
}
