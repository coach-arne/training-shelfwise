import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { catalogueApi } from '../../core/api/catalogue.api';
import { MaterialType, TitleRequest, TitleResult } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { MATERIAL_LABEL } from '../../helpers/labels';
import { notifySuccess } from '../../store/notify.store';
import { ErrorMessage } from '../../shared/error-message';

/** Create a title or change its bibliographic data (F-CAT-01). */
@Component({
  selector: 'app-title-form',
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, ErrorMessage],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" (ngSubmit)="save()" class="grid">
      <mat-form-field><mat-label>Title</mat-label><input matInput formControlName="title" data-testid="title-title" /></mat-form-field>
      <mat-form-field><mat-label>Authors</mat-label><input matInput formControlName="authors" data-testid="title-authors" /></mat-form-field>
      <mat-form-field><mat-label>Publisher</mat-label><input matInput formControlName="publisher" /></mat-form-field>
      <mat-form-field><mat-label>Publication year</mat-label><input matInput type="number" formControlName="publicationYear" /></mat-form-field>
      <mat-form-field><mat-label>ISBN</mat-label><input matInput formControlName="isbn" /></mat-form-field>
      <mat-form-field>
        <mat-label>Material type</mat-label>
        <mat-select formControlName="materialType" data-testid="title-material">
          @for (type of materialTypes; track type) {
            <mat-option [value]="type">{{ label(type) }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <div class="full">
        <app-error-message [error]="saver.error()" />
        <button mat-flat-button type="submit" [disabled]="form.invalid || saver.loading()" data-testid="title-save">{{ title() ? 'Save title' : 'Create title' }}</button>
        <button mat-button type="button" (click)="cancelled.emit()">Cancel</button>
      </div>
    </form>
  `,
  styles: `
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 4px 16px; align-items: center; }
    .full { grid-column: 1 / -1; }
  `,
})
export class TitleForm {
  private readonly fb = inject(FormBuilder).nonNullable;

  readonly title = input<TitleResult | null>(null);
  readonly saved = output<TitleResult>();
  readonly cancelled = output<void>();

  protected readonly materialTypes: MaterialType[] = ['BOOK', 'AUDIOVISUAL', 'MAGAZINE'];
  protected readonly label = (type: MaterialType) => MATERIAL_LABEL[type];

  protected readonly form = this.fb.group({
    title: ['', Validators.required],
    authors: ['', Validators.required],
    publisher: [''],
    publicationYear: [null as number | null],
    isbn: [''],
    materialType: ['BOOK' as MaterialType, Validators.required],
  });

  protected readonly saver = createAction(async () => {
    const value = this.form.getRawValue();
    const request: TitleRequest = {
      title: value.title.trim(),
      authors: value.authors.trim(),
      publisher: value.publisher.trim() || null,
      publicationYear: value.publicationYear,
      isbn: value.isbn.trim() || null,
      materialType: value.materialType,
    };
    const existing = this.title();
    const result = existing ? await catalogueApi.updateTitle(existing.id, request) : await catalogueApi.createTitle(request);
    notifySuccess('Title saved.');
    this.saved.emit(result);
  });

  constructor() {
    effect(() => {
      const existing = this.title();
      this.form.reset({
        title: existing?.title ?? '',
        authors: existing?.authors ?? '',
        publisher: existing?.publisher ?? '',
        publicationYear: existing?.publicationYear ?? null,
        isbn: existing?.isbn ?? '',
        materialType: existing?.materialType ?? 'BOOK',
      });
    });
  }

  protected save(): void {
    void this.saver.run();
  }
}
