import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { ReservationView } from '../core/models';
import { RESERVATION_STATUS_LABEL } from '../helpers/labels';
import { RESERVATION_STATUS_TONE } from '../helpers/tones';
import { StatusPill } from '../ui/status-pill';

@Component({
  selector: 'app-reservation-table',
  imports: [DatePipe, MatButtonModule, StatusPill],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (reservations().length === 0) {
      <p class="empty">No reservations.</p>
    } @else {
      <table class="data-table" data-testid="reservations-table">
        <thead>
          <tr><th>Title</th><th>Pickup</th><th>Status</th><th>Queue</th><th>Hold until</th><th></th></tr>
        </thead>
        <tbody>
          @for (reservation of reservations(); track reservation.id) {
            <tr>
              <td>{{ reservation.title }}</td>
              <td>{{ reservation.pickupBranchCode }}</td>
              <td>
                <app-status-pill [tone]="tone(reservation)">{{ statusLabel(reservation) }}</app-status-pill>
                @if (reservation.cancelledByLibrary) { <small>(by the library)</small> }
              </td>
              <td>{{ reservation.queuePosition ?? '' }}</td>
              <td>{{ reservation.holdUntil | date: 'dd MMM yyyy' }}</td>
              <td>
                @if (isActive(reservation)) {
                  <button mat-stroked-button (click)="cancel.emit(reservation)" [attr.data-testid]="'cancel-reservation-' + reservation.id">Cancel</button>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class ReservationTable {
  readonly reservations = input.required<ReservationView[]>();
  readonly cancel = output<ReservationView>();

  protected tone(reservation: ReservationView) {
    return RESERVATION_STATUS_TONE[reservation.status];
  }

  protected statusLabel(reservation: ReservationView): string {
    return RESERVATION_STATUS_LABEL[reservation.status];
  }

  protected isActive(reservation: ReservationView): boolean {
    return reservation.status === 'WAITING' || reservation.status === 'ON_HOLD';
  }
}
