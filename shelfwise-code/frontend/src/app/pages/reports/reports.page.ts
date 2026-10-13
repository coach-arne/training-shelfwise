import { ChangeDetectionStrategy, Component, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { reportsApi } from '../../core/api/admin.api';
import { catalogueApi } from '../../core/api/catalogue.api';
import { BranchView, CirculationRow, OverdueRow } from '../../core/models';
import { createAction } from '../../helpers/async-action';
import { MATERIAL_LABEL } from '../../helpers/labels';
import { isMember, profile, role } from '../../store/auth.store';
import { systemInfo } from '../../store/system.store';
import { ErrorMessage } from '../../shared/error-message';
import { MoneyPipe } from '../../shared/money.pipe';
import { SectionCard } from '../../ui/section-card';
import { PageHeader } from '../../ui/page-header';

/** R-01 daily circulation and R-02 overdue items (section 13) with CSV export. Librarians see their own branch. */
@Component({
  selector: 'app-reports-page',
  imports: [PageHeader, SectionCard, 
    DatePipe, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatTabsModule,
    ErrorMessage, MoneyPipe,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <app-page-header eyebrow="Management" title="Reports" subtitle="Daily circulation and overdue items." />
      @if (isLibrarian()) {
        <p class="info">Showing your own branch ({{ ownBranch() }}).</p>
      } @else {
        <mat-form-field>
          <mat-label>Branch</mat-label>
          <mat-select [value]="branchId()" (valueChange)="branchId.set($event)" data-testid="report-branch">
            <mat-option [value]="null">All branches</mat-option>
            @for (branch of branches(); track branch.id) {
              <mat-option [value]="branch.id">{{ branch.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      }
      <app-error-message [error]="circulationLoader.error() ?? overdueLoader.error() ?? exporter.error()" />

      <mat-tab-group animationDuration="0ms">
        <mat-tab label="R-01 Daily circulation">
          <app-section-card>
            <div class="row">
              <mat-form-field>
                <mat-label>Date</mat-label>
                <input matInput type="date" [ngModel]="date()" (ngModelChange)="date.set($event)" data-testid="r01-date" />
              </mat-form-field>
              <button mat-flat-button (click)="loadCirculation()" data-testid="r01-run">Show</button>
              <button mat-stroked-button (click)="exportCirculation()" data-testid="r01-csv">Download CSV</button>
            </div>
            @if (circulation().length === 0) {
              <p class="empty">No circulation on this date.</p>
            } @else {
              <table class="data-table" data-testid="r01-table">
                <thead><tr><th>Branch</th><th>Material</th><th class="num">Check-outs</th><th class="num">Returns</th><th class="num">Renewals</th></tr></thead>
                <tbody>
                  @for (row of circulation(); track row.branch + row.materialType) {
                    <tr><td>{{ row.branch }}</td><td>{{ material(row.materialType) }}</td><td class="num">{{ row.checkouts }}</td><td class="num">{{ row.returns }}</td><td class="num">{{ row.renewals }}</td></tr>
                  }
                </tbody>
              </table>
            }
          </app-section-card>
        </mat-tab>
        <mat-tab label="R-02 Overdue items">
          <app-section-card>
            <div class="row">
              <button mat-flat-button (click)="loadOverdue()" data-testid="r02-run">Show</button>
              <button mat-stroked-button (click)="exportOverdue()" data-testid="r02-csv">Download CSV</button>
            </div>
            @if (overdue().length === 0) {
              <p class="empty">No overdue items.</p>
            } @else {
              <table class="data-table" data-testid="r02-table">
                <thead><tr><th>Member</th><th>Card</th><th>Title</th><th>Barcode</th><th>Due</th><th class="num">Days overdue</th><th class="num">Accrued fine</th><th>Branch</th></tr></thead>
                <tbody>
                  @for (row of overdue(); track row.loanId) {
                    <tr>
                      <td>{{ row.memberName }}</td><td>{{ row.cardNumber }}</td><td>{{ row.title }}</td><td>{{ row.barcode }}</td>
                      <td>{{ row.dueDate | date: 'dd MMM yyyy' }}</td><td class="num">{{ row.daysOverdue }}</td>
                      <td class="num">{{ row.accruedFine | money }}</td><td>{{ row.branch }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            }
          </app-section-card>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: `.info { color: var(--sw-color-muted); }`,
})
export class ReportsPage implements OnInit {
  protected readonly branches = signal<BranchView[]>([]);
  protected readonly branchId = signal<number | null>(null);
  protected readonly date = signal(systemInfo()?.businessDate ?? '');
  protected readonly circulation = signal<CirculationRow[]>([]);
  protected readonly overdue = signal<OverdueRow[]>([]);
  protected readonly isLibrarian = () => role() === 'LIBRARIAN';
  protected readonly ownBranch = () => profile()?.branchCode ?? '';
  protected readonly material = (type: keyof typeof MATERIAL_LABEL) => MATERIAL_LABEL[type];

  /** A librarian always gets their own branch from the server, so no branch is sent. */
  private branchParam(): number | null {
    return this.isLibrarian() ? null : this.branchId();
  }

  protected readonly circulationLoader = createAction(async () => {
    this.circulation.set(await reportsApi.circulation(this.date() || null, this.branchParam()));
  });

  protected readonly overdueLoader = createAction(async () => {
    this.overdue.set(await reportsApi.overdue(this.branchParam()));
  });

  protected readonly exporter = createAction(async (task: () => Promise<void>) => task());

  ngOnInit(): void {
    if (!isMember()) {
      catalogueApi.branches().then((list) => this.branches.set(list)).catch(() => this.branches.set([]));
    }
    void this.circulationLoader.run();
    void this.overdueLoader.run();
  }

  protected loadCirculation(): void {
    void this.circulationLoader.run();
  }

  protected loadOverdue(): void {
    void this.overdueLoader.run();
  }

  protected exportCirculation(): void {
    void this.exporter.run(() => reportsApi.downloadCirculation(this.date() || null, this.branchParam()));
  }

  protected exportOverdue(): void {
    void this.exporter.run(() => reportsApi.downloadOverdue(this.branchParam()));
  }
}
