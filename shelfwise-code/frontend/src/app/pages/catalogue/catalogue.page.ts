import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { catalogueApi } from '../../core/api/catalogue.api';
import { deskApi } from '../../core/api/desk.api';
import { portalApi } from '../../core/api/portal.api';
import { BranchView, SearchType, TitleResult } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { isDesk, isMember } from '../../store/auth.store';
import { contextMember, refreshMemberContext } from '../../store/member-context.store';
import { notifySuccess } from '../../store/notify.store';
import { refreshPortal } from '../../store/portal.store';
import { CopyStatusChip } from '../../shared/copy-status-chip';
import { ErrorMessage } from '../../shared/error-message';
import { ReserveAction, ReserveRequest } from '../../shared/reserve-action';
import { TitleResultCard } from '../../shared/title-result-card';

/**
 * The public catalogue (F-CAT-04): search by title, author or ISBN with availability per branch.
 * Members can reserve here, librarians can reserve for the member they are serving, and staff also see the copies.
 */
@Component({
  selector: 'app-catalogue-page',
  imports: [
    FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, RouterLink, CopyStatusChip,
    ErrorMessage, ReserveAction, TitleResultCard,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <h1>Catalogue</h1>
      <form class="row" (ngSubmit)="search()">
        <mat-form-field class="query">
          <mat-label>Search</mat-label>
          <input matInput name="q" [ngModel]="query()" (ngModelChange)="query.set($event)" data-testid="catalogue-query"
                 placeholder="Title, author or ISBN" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Search in</mat-label>
          <mat-select name="type" [value]="type()" (valueChange)="type.set($event)">
            <mat-option value="ALL">Everything</mat-option>
            <mat-option value="TITLE">Title</mat-option>
            <mat-option value="AUTHOR">Author</mat-option>
            <mat-option value="ISBN">ISBN</mat-option>
          </mat-select>
        </mat-form-field>
        <button mat-flat-button type="submit" [disabled]="finder.loading()" data-testid="catalogue-search">Search</button>
      </form>

      <app-error-message [error]="finder.error() ?? booking.error()" />
      @if (reservingFor(); as target) {
        <div class="warning-box">Reservations made here are for <strong>{{ target }}</strong>.</div>
      }

      @if (searched() && results().length === 0) {
        <p class="empty" data-testid="no-results">Nothing found.</p>
      }
      @for (title of results(); track title.id) {
        <app-title-result-card [title]="title">
          @if (title.copies; as copies) {
            <table class="data-table">
              <thead><tr><th>Barcode</th><th>Home</th><th>Now at</th><th>Status</th></tr></thead>
              <tbody>
                @for (copy of copies; track copy.id) {
                  <tr>
                    <td>{{ copy.barcode }}</td>
                    <td>{{ copy.homeBranchCode }}</td>
                    <td>{{ copy.currentBranchCode }}@if (copy.destinationBranchCode) { → {{ copy.destinationBranchCode }} }</td>
                    <td><app-copy-status-chip [status]="copy.status" /></td>
                  </tr>
                }
              </tbody>
            </table>
          }
          @if (canReserve()) {
            <app-reserve-action [titleId]="title.id" [branches]="branches()" [busy]="booking.loading()"
                                (reserve)="reserve($event)" />
          } @else if (!loggedIn()) {
            <p><a routerLink="/login">Log in</a> to reserve this title.</p>
          }
        </app-title-result-card>
      }
    </div>
  `,
  styles: `.query { min-width: 320px; }`,
})
export class CataloguePage {
  protected readonly query = signal('');
  protected readonly type = signal<SearchType>('ALL');
  protected readonly results = signal<TitleResult[]>([]);
  protected readonly branches = signal<BranchView[]>([]);
  protected readonly searched = signal(false);

  protected readonly loggedIn = () => isMember() || isDesk();
  protected readonly canReserve = () => isMember() || (isDesk() && contextMember() !== null);
  protected readonly reservingFor = () => {
    const member = contextMember();
    return isDesk() && member ? `${member.firstName} ${member.lastName}` : null;
  };

  protected readonly finder = createAction(async () => {
    this.results.set(await catalogueApi.search(this.query().trim(), this.type()));
    this.searched.set(true);
  });

  protected readonly booking = createAction(async (request: ReserveRequest) => {
    const member = contextMember();
    if (isDesk() && member) {
      await deskApi.reserve(member.id, request.titleId, request.pickupBranchId);
      await refreshMemberContext();
    } else {
      await portalApi.reserve(request.titleId, request.pickupBranchId);
      await refreshPortal();
    }
    notifySuccess('Reservation placed.');
    await this.finder.run();
  });

  constructor() {
    catalogueApi.branches().then((list) => this.branches.set(list)).catch(() => this.branches.set([]));
  }

  protected search(): void {
    void this.finder.run();
  }

  protected reserve(request: ReserveRequest): void {
    void this.booking.run(request);
  }
}
