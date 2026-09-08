import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { apiErrorMessage } from '../../core/http/api-error';
import { Loan, Page } from '../../core/models';
import { LoansService } from './loans.service';

@Component({
  selector: 'app-active-loans',
  imports: [DatePipe],
  template: `
    <div class="space-y-4">
      <h1 class="font-serif text-2xl font-bold text-ink">Préstamos activos</h1>
      <p class="text-sm text-muted">Quién tiene cada libro prestado en este momento.</p>

      @if (error()) {
        <p class="rounded bg-[#f6e7e3] p-2 text-sm text-out">{{ error() }}</p>
      }
      @if (message()) {
        <p class="rounded bg-[#e7efe7] p-2 text-sm text-ok">{{ message() }}</p>
      }

      @if (loading()) {
        <p class="text-muted">Cargando…</p>
      } @else if (data(); as p) {
        @if (p.content.length === 0) {
          <p class="text-muted">No hay préstamos activos.</p>
        } @else {
          <div class="overflow-x-auto rounded border border-line bg-surface">
            <table class="w-full text-left text-sm">
              <thead class="bg-paper font-serif text-muted">
                <tr>
                  <th class="px-4 py-2">Libro</th>
                  <th class="px-4 py-2">Lo tiene</th>
                  <th class="px-4 py-2">Correo</th>
                  <th class="px-4 py-2">Vence</th>
                  <th class="px-4 py-2">Estado</th>
                  <th class="px-4 py-2"></th>
                </tr>
              </thead>
              <tbody>
                @for (loan of p.content; track loan.id) {
                  <tr class="border-t border-line">
                    <td class="px-4 py-2 font-medium text-ink">{{ loan.bookTitle }}</td>
                    <td class="px-4 py-2">{{ loan.borrowerName }}</td>
                    <td class="px-4 py-2 font-mono text-xs text-muted">{{ loan.borrowerEmail }}</td>
                    <td class="px-4 py-2 font-mono text-xs">{{ loan.dueDate | date: 'dd/MM/yyyy' }}</td>
                    <td class="px-4 py-2">
                      @if (loan.overdue) {
                        <span class="rounded-full bg-[#f6e7e3] px-2 py-0.5 text-xs font-semibold text-out">Vencido</span>
                      } @else {
                        <span class="rounded-full bg-[#e7efe7] px-2 py-0.5 text-xs font-semibold text-ok">Vigente</span>
                      }
                    </td>
                    <td class="px-4 py-2 text-right">
                      <button type="button" (click)="returnLoan(loan)"
                        class="rounded bg-forest px-3 py-1 text-xs font-medium text-white hover:bg-forest/90">
                        Registrar devolución
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <!-- Paginación -->
          <div class="flex items-center justify-between text-sm text-muted">
            <span>{{ p.totalElements }} préstamo(s) · página {{ p.page + 1 }} de {{ totalPages() }}</span>
            <div class="flex gap-2">
              <button type="button" (click)="go(p.page - 1)" [disabled]="p.page === 0"
                class="rounded border border-line px-3 py-1 hover:bg-forest/5 disabled:opacity-40">Anterior</button>
              <button type="button" (click)="go(p.page + 1)" [disabled]="p.page + 1 >= totalPages()"
                class="rounded border border-line px-3 py-1 hover:bg-forest/5 disabled:opacity-40">Siguiente</button>
            </div>
          </div>
        }
      }
    </div>
  `,
})
export class ActiveLoans implements OnInit {
  private readonly loansService = inject(LoansService);

  private readonly size = 10;
  protected readonly data = signal<Page<Loan> | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly totalPages = computed(() => Math.max(1, this.data()?.totalPages ?? 1));

  ngOnInit(): void {
    this.go(0);
  }

  go(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.loansService.active(page, this.size).subscribe({
      next: (p) => {
        this.data.set(p);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(e, 'No se pudieron cargar los préstamos.'));
      },
    });
  }

  returnLoan(loan: Loan): void {
    this.loansService.return(loan.id).subscribe({
      next: () => {
        this.message.set(`Devolución registrada: ${loan.bookTitle}.`);
        this.error.set(null);
        this.go(this.data()?.page ?? 0);
      },
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudo registrar la devolución.')),
    });
  }
}
