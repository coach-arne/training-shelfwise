import { ChangeDetectionStrategy, Component, effect, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { catalogueApi } from '../../core/api/catalogue.api';
import { deskApi } from '../../core/api/desk.api';
import { portalApi } from '../../core/api/portal.api';
import { BranchView, SearchType, TitleResult } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { DebouncedSearch, watchDebouncedSearch } from '../../helpers/debounced-search';
import { isDesk, isMember } from '../../store/auth.store';
import { contextMember, refreshMemberContext } from '../../store/member-context.store';
import { notifySuccess } from '../../store/notify.store';
import { refreshPortal } from '../../store/portal.store';
import { CopyStatusChip } from '../../shared/copy-status-chip';
import { ErrorMessage } from '../../shared/error-message';
import { ReserveAction, ReserveRequest } from '../../shared/reserve-action';
import { TitleResultCard } from '../../shared/title-result-card';
import { EmptyState } from '../../ui/empty-state';
import { Icon } from '../../ui/icon';
import { PageHeader } from '../../ui/page-header';

/**
 * The public catalogue (F-CAT-04): search by title, author or ISBN with availability per branch.
 * Members can reserve here, librarians can reserve for the member they are serving, and staff also see the copies.
 */
@Component({
  selector: 'app-catalogue-page',
  imports: [
    FormsModule, MatButtonModule, RouterLink, CopyStatusChip, EmptyState, ErrorMessage, Icon, PageHeader, ReserveAction,
    TitleResultCard,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <app-page-header eyebrow="Northwick libraries" title="Catalogue" subtitle="Search by title, author or ISBN and see what is on the shelf at every branch." />
      <form class="searchbar" (ngSubmit)="search()">
        <app-icon name="search" />
        <input name="q" [ngModel]="query()" (ngModelChange)="onQueryChange($event)" data-testid="catalogue-query"
               placeholder="Title, author or ISBN" aria-label="Search" />
        <select name="type" [ngModel]="type()" (ngModelChange)="onTypeChange($event)" aria-label="Search in">
          <option value="ALL">Everything</option>
          <option value="TITLE">Title</option>
          <option value="AUTHOR">Author</option>
          <option value="ISBN">ISBN</option>
        </select>
        <button mat-flat-button type="submit" [disabled]="finder.loading()" data-testid="catalogue-search">Search</button>
      </form>

      <app-error-message [error]="finder.error() ?? booking.error()" />
      @if (reservingFor(); as target) {
        <div class="info-box">Reservations made here are for <strong>{{ target }}</strong>.</div>
      }

      @if (searched() && results().length === 0) {
        <app-empty-state icon="search"><span data-testid="no-results">Nothing found. Try a shorter search.</span></app-empty-state>
      }
      @for (title of results(); track title.id) {
        <app-title-result-card [title]="title">
          @if (title.copies; as copies) {
            <table class="data-table">
              <thead><tr><th>Barcode</th><th>Home</th><th>Now at</th><th>Status</th></tr></thead>
              <tbody>
                @for (copy of copies; track copy.id) {
                  <tr>
                    <td class="mono">{{ copy.barcode }}</td>
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
  styles: `
    .searchbar { display: flex; align-items: center; gap: var(--sw-space-3); padding: 6px 6px 6px var(--sw-space-4); margin-bottom: var(--sw-space-4);
                 background: var(--sw-color-surface); border: 1px solid var(--sw-color-line-strong); border-radius: var(--sw-radius-pill); box-shadow: var(--sw-shadow-card); color: var(--sw-color-muted); }
    .searchbar:focus-within { border-color: var(--sw-color-accent); box-shadow: 0 0 0 3px var(--sw-color-accent-soft); }
    .searchbar input { flex: 1; min-width: 0; border: 0; outline: 0; background: transparent; font: inherit; font-size: 16px; color: var(--sw-color-ink); }
    .searchbar select { border: 0; border-left: 1px solid var(--sw-color-line); padding: 6px 8px 6px var(--sw-space-3); background: transparent; font: inherit; font-size: 14px; color: var(--sw-color-muted); outline: 0; cursor: pointer; }
  `,
})
export class CataloguePage {
  /** `?q=` from the URL (for instance from the training kit) is searched straight away. */
  readonly q = input<string>();

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

  private readonly auto: DebouncedSearch;

  constructor() {
    this.auto = watchDebouncedSearch(
      () => [this.query(), this.type()],
      () => this.applySearch(),
    );
    effect(() => {
      const fromUrl = this.q();
      if (fromUrl) {
        untracked(() => {
          this.auto.suppressNext();
          this.query.set(fromUrl);
          this.type.set('ALL');
          void this.finder.run();
        });
      }
    });
    catalogueApi.branches().then((list) => this.branches.set(list)).catch(() => this.branches.set([]));
  }

  protected onQueryChange(value: string): void {
    this.query.set(value);
    this.auto.notify();
  }

  protected onTypeChange(value: SearchType): void {
    this.type.set(value);
    this.auto.notify();
  }

  protected search(): void {
    this.auto.cancelPending();
    this.applySearch();
  }

  private applySearch(): void {
    if (!this.query().trim()) {
      this.results.set([]);
      this.searched.set(false);
      return;
    }
    void this.finder.run();
  }

  protected reserve(request: ReserveRequest): void {
    void this.booking.run(request);
  }
}
