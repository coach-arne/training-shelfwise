import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { catalogueApi } from '../../core/api/catalogue.api';
import { BranchView, TitleResult } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { ErrorMessage } from '../../shared/error-message';
import { TitleResultCard } from '../../shared/title-result-card';
import { CopyAdmin } from './copy-admin';
import { TitleForm } from './title-form';

/** Maintain titles and copies (F-CAT-01, F-CAT-03). */
@Component({
  selector: 'app-title-maintenance-page',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, ErrorMessage, TitleResultCard, CopyAdmin, TitleForm],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <h1>Titles &amp; copies</h1>
      <form class="row" (ngSubmit)="search()">
        <mat-form-field class="query">
          <mat-label>Search titles</mat-label>
          <input matInput name="q" [ngModel]="query()" (ngModelChange)="query.set($event)" data-testid="maintenance-query" />
        </mat-form-field>
        <button mat-flat-button type="submit" [disabled]="finder.loading()" data-testid="maintenance-search">Search</button>
        <button mat-stroked-button type="button" (click)="startCreate()" data-testid="new-title">New title</button>
      </form>
      <app-error-message [error]="finder.error()" />

      @if (creating()) {
        <div class="panel">
          <h2>New title</h2>
          <app-title-form (saved)="created($event)" (cancelled)="creating.set(false)" />
        </div>
      }

      @for (title of results(); track title.id) {
        <app-title-result-card [title]="title">
          @if (editingId() === title.id) {
            <app-title-form [title]="title" (saved)="edited()" (cancelled)="editingId.set(null)" />
          } @else {
            <button mat-stroked-button (click)="editingId.set(title.id)" [attr.data-testid]="'edit-title-' + title.id">Edit title</button>
          }
          <app-copy-admin [title]="title" [branches]="branches()" (changed)="reload()" />
        </app-title-result-card>
      }
    </div>
  `,
  styles: `.query { min-width: 320px; }`,
})
export class TitleMaintenancePage {
  protected readonly query = signal('');
  protected readonly results = signal<TitleResult[]>([]);
  protected readonly branches = signal<BranchView[]>([]);
  protected readonly creating = signal(false);
  protected readonly editingId = signal<number | null>(null);

  protected readonly finder = createAction(async () => {
    this.results.set(await catalogueApi.search(this.query().trim(), 'ALL'));
  });

  constructor() {
    catalogueApi.branches().then((list) => this.branches.set(list)).catch(() => this.branches.set([]));
  }

  protected search(): void {
    void this.finder.run();
  }

  protected reload(): void {
    void this.finder.run();
  }

  protected startCreate(): void {
    this.creating.set(true);
  }

  protected created(title: TitleResult): void {
    this.creating.set(false);
    this.query.set(title.title);
    void this.finder.run();
  }

  protected edited(): void {
    this.editingId.set(null);
    void this.finder.run();
  }
}
