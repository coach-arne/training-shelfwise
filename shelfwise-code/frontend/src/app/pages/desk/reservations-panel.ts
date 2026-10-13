import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { catalogueApi } from '../../core/api/catalogue.api';
import { deskApi } from '../../core/api/desk.api';
import { BranchView, ReservationView, TitleResult } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { contextMember, contextReservations, refreshMemberContext } from '../../store/member-context.store';
import { notifySuccess } from '../../store/notify.store';
import { ErrorMessage } from '../../shared/error-message';
import { ReservationTable } from '../../shared/reservation-table';
import { ReserveAction, ReserveRequest } from '../../shared/reserve-action';
import { TitleResultCard } from '../../shared/title-result-card';

/** Reservations of the member: cancel, or search a title and reserve it for the member (F-RES-01, F-RES-05). */
@Component({
  selector: 'app-reservations-panel',
  imports: [
    FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, ErrorMessage, ReservationTable, ReserveAction,
    TitleResultCard,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-error-message [error]="canceller.error()" />
    <app-reservation-table [reservations]="reservations()" (cancel)="cancel($event)" />

    <h3>Reserve a title for this member</h3>
    <form class="row" (ngSubmit)="search()">
      <mat-form-field>
        <mat-label>Search the catalogue</mat-label>
        <input matInput name="q" [ngModel]="query()" (ngModelChange)="query.set($event)" data-testid="reserve-query" />
      </mat-form-field>
      <button mat-stroked-button type="submit" [disabled]="finder.loading()" data-testid="reserve-search">Search</button>
    </form>
    <app-error-message [error]="finder.error() ?? booker.error()" />
    @for (title of results(); track title.id) {
      <app-title-result-card [title]="title">
        <app-reserve-action [titleId]="title.id" [branches]="branches()" [busy]="booker.loading()" (reserve)="reserve($event)" />
      </app-title-result-card>
    }
  `,
})
export class ReservationsPanel {
  protected readonly reservations = contextReservations;
  protected readonly query = signal('');
  protected readonly results = signal<TitleResult[]>([]);
  protected readonly branches = signal<BranchView[]>([]);

  protected readonly canceller = createAction(async (reservation: ReservationView) => {
    await deskApi.cancelReservation(reservation.id);
    await refreshMemberContext();
    notifySuccess('Reservation cancelled. The reservation fee is not refunded.');
  });

  protected readonly finder = createAction(async () => {
    this.results.set(await catalogueApi.search(this.query().trim(), 'ALL'));
  });

  protected readonly booker = createAction(async (request: ReserveRequest) => {
    const member = contextMember();
    if (!member) {
      return;
    }
    await deskApi.reserve(member.id, request.titleId, request.pickupBranchId);
    await refreshMemberContext();
    notifySuccess('Reservation placed.');
    await this.finder.run();
  });

  constructor() {
    catalogueApi.branches().then((list) => this.branches.set(list)).catch(() => this.branches.set([]));
  }

  protected cancel(reservation: ReservationView): void {
    void this.canceller.run(reservation);
  }

  protected search(): void {
    void this.finder.run();
  }

  protected reserve(request: ReserveRequest): void {
    void this.booker.run(request);
  }
}
