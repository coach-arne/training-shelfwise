import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DemoBook, DemoCopy } from '../core/models';
import { copyToClipboard } from '../helpers/clipboard';
import { COPY_STATUS_LABEL } from '../helpers/labels';
import { COPY_STATUS_TONE } from '../helpers/tones';
import { filterBooks } from '../helpers/training-filter';
import { notifySuccess } from '../store/notify.store';
import { pickBarcode } from '../store/training-kit.store';
import { TrainingFilter } from './training-filter';

/**
 * The example books of the seed catalogue. Choosing a title searches the catalogue for it;
 * choosing a barcode copies it and fills the barcode field of the checkout and returns screens.
 */
@Component({
  selector: 'app-demo-book-list',
  imports: [TrainingFilter],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-training-filter placeholder="Filter: title, author, ISBN, barcode…" [value]="query()" (valueChange)="query.set($event)" />
    <div class="list">
      @for (book of visible(); track book.titleId) {
        <div class="book" [attr.data-testid]="'demo-book-' + book.titleId">
          <button type="button" class="title" (click)="search(book)">
            <strong>{{ book.title }}</strong>
            <small>{{ book.authors }}</small>
          </button>
          <div class="copies">
            @for (copy of book.copies; track copy.barcode) {
              <button type="button" class="copy" (click)="pick(copy)" [title]="statusLabel(copy) + ' at ' + copy.currentBranchCode">
                <span class="dot" [class]="'tone-' + tone(copy)"></span>
                <span class="mono">{{ copy.barcode }}</span>
              </button>
            }
          </div>
        </div>
      } @empty {
        <p class="none">Nothing matches.</p>
      }
    </div>
    <p class="hint">Click a title to search for it. Click a barcode to copy it into the desk fields.</p>
  `,
  styles: `
    :host { display: flex; flex-direction: column; min-height: 0; gap: var(--sw-space-2); }
    .list { overflow-y: auto; min-height: 0; margin: 0 calc(var(--sw-space-2) * -1); padding: 0 var(--sw-space-2); }
    .book { padding: var(--sw-space-2) var(--sw-space-3); border-radius: var(--sw-radius-sm); }
    .book:hover { background: var(--sw-color-sunken); }
    .title { display: flex; flex-direction: column; text-align: left; padding: 0; border: 0; background: transparent; color: inherit; font: inherit; cursor: pointer; }
    .title:hover strong { text-decoration: underline; text-underline-offset: 3px; }
    .title small { font-size: 12px; }
    .copies { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
    .copy { display: inline-flex; align-items: center; gap: 6px; padding: 1px 8px; border: 1px solid var(--sw-color-line-strong); border-radius: var(--sw-radius-pill);
            background: var(--sw-color-surface); color: var(--sw-color-ink); font-size: 11px; cursor: pointer; }
    .copy:hover { border-color: var(--sw-color-accent); }
    .dot { width: 7px; height: 7px; border-radius: 50%; }
    .dot.tone-success { background: var(--sw-color-success); }
    .dot.tone-info { background: var(--sw-color-info); }
    .dot.tone-warning { background: var(--sw-color-warning-accent); }
    .dot.tone-danger { background: var(--sw-color-danger); }
    .dot.tone-neutral { background: var(--sw-color-neutral); }
    .none, .hint { margin: var(--sw-space-3) 0 0; font-size: 12px; color: var(--sw-color-muted); }
  `,
})
export class DemoBookList {
  readonly books = input.required<DemoBook[]>();
  readonly closed = output<void>();

  private readonly router = inject(Router);
  protected readonly query = signal('');
  protected readonly visible = computed(() => filterBooks(this.books(), this.query()));
  protected readonly tone = (copy: DemoCopy) => COPY_STATUS_TONE[copy.status];
  protected readonly statusLabel = (copy: DemoCopy) => COPY_STATUS_LABEL[copy.status];

  protected search(book: DemoBook): void {
    void this.router.navigate(['/catalogue'], { queryParams: { q: book.title } });
    this.closed.emit();
  }

  protected async pick(copy: DemoCopy): Promise<void> {
    pickBarcode(copy.barcode);
    const copied = await copyToClipboard(copy.barcode);
    notifySuccess(copied ? `Barcode ${copy.barcode} copied.` : `Barcode ${copy.barcode} picked.`);
  }
}
