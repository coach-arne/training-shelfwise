import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { DatePipe } from '@angular/common';
import { deskApi } from '../../core/api/desk.api';
import { MemberSummary } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { DebouncedSearch, watchDebouncedSearch } from '../../helpers/debounced-search';
import { CATEGORY_LABEL } from '../../helpers/labels';
import { selectMember } from '../../store/member-context.store';
import { ErrorMessage } from '../../shared/error-message';
import { StatusPill } from '../../ui/status-pill';

const CARD_NUMBER = /^NPL\d+$/i;

/** Find a member by card number, name, or e-mail address and open the member at the desk. */
@Component({
  selector: 'app-member-lookup',
  imports: [FormsModule, DatePipe, MatButtonModule, MatFormFieldModule, MatInputModule, ErrorMessage, StatusPill],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="row" (ngSubmit)="search()">
      <mat-form-field class="query">
        <mat-label>Card number or name</mat-label>
        <input matInput name="q" [ngModel]="query()" (ngModelChange)="onQueryChange($event)" data-testid="member-query" autofocus />
      </mat-form-field>
      <button mat-flat-button type="submit" [disabled]="finder.loading()" data-testid="member-search">Find member</button>
    </form>
    <app-error-message [error]="finder.error()" />
    @if (results().length > 1) {
      <table class="data-table" data-testid="member-results">
        <thead><tr><th>Card</th><th>Name</th><th>Born</th><th>Category</th><th></th></tr></thead>
        <tbody>
          @for (member of results(); track member.id) {
            <tr>
              <td class="mono">{{ member.cardNumber }}</td>
              <td>{{ member.fullName }} @if (member.blocked) { <app-status-pill tone="danger">Blocked</app-status-pill> }</td>
              <td>{{ member.birthDate | date: 'dd MMM yyyy' }}</td>
              <td>{{ category(member) }}</td>
              <td><button mat-stroked-button (click)="open(member.id)" [attr.data-testid]="'open-member-' + member.cardNumber">Open</button></td>
            </tr>
          }
        </tbody>
      </table>
    }
  `,
  styles: `
    .query { min-width: 320px; }
  `,
})
export class MemberLookup {
  protected readonly query = signal('');
  protected readonly results = signal<MemberSummary[]>([]);
  protected readonly category = (member: MemberSummary) => CATEGORY_LABEL[member.category];

  protected readonly finder = createAction(async () => {
    const text = this.query().trim();
    if (!text) {
      return;
    }
    if (CARD_NUMBER.test(text)) {
      const member = await deskApi.memberByCard(text.toUpperCase());
      this.results.set([]);
      await selectMember(member.id);
      return;
    }
    const found = await deskApi.searchMembers(text);
    this.results.set(found);
    if (found.length === 1) {
      await selectMember(found[0].id);
    }
  });

  private readonly auto: DebouncedSearch;

  constructor() {
    this.auto = watchDebouncedSearch(
      () => this.query(),
      () => this.applySearch(),
    );
  }

  protected onQueryChange(value: string): void {
    this.query.set(value);
    this.auto.notify();
  }

  protected search(): void {
    this.auto.cancelPending();
    this.applySearch();
  }

  private applySearch(): void {
    if (!this.query().trim()) {
      this.results.set([]);
      return;
    }
    void this.finder.run();
  }

  protected open(id: number): void {
    this.results.set([]);
    void selectMember(id);
  }
}
