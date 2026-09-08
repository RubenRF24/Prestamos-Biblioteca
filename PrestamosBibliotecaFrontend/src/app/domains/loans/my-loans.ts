import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { apiErrorMessage } from '../../core/http/api-error';
import { Loan } from '../../core/models';
import { LoansService } from './loans.service';

@Component({
  selector: 'app-my-loans',
  imports: [DatePipe],
  template: `
    <div class="space-y-4">
      <h1 class="font-serif text-2xl font-bold text-ink">Mis préstamos</h1>
      <p class="text-sm text-muted">La devolución se registra en la biblioteca al entregar el libro.</p>

      @if (error()) {
        <p class="rounded bg-[#f6e7e3] p-2 text-sm text-out">{{ error() }}</p>
      }

      @if (loading()) {
        <p class="text-muted">Cargando…</p>
      } @else if (loans().length === 0) {
        <p class="text-muted">Todavía no tenés préstamos.</p>
      } @else {
        <div class="overflow-x-auto rounded border border-line bg-surface">
          <table class="w-full text-left text-sm">
            <thead class="bg-paper font-serif text-muted">
              <tr>
                <th class="px-4 py-2">Libro</th>
                <th class="px-4 py-2">Prestado</th>
                <th class="px-4 py-2">Vence</th>
                <th class="px-4 py-2">Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (loan of loans(); track loan.id) {
                <tr class="border-t border-line">
                  <td class="px-4 py-2 font-medium text-ink">{{ loan.bookTitle }}</td>
                  <td class="px-4 py-2 font-mono text-xs text-muted">{{ loan.loanDate | date: 'dd/MM/yyyy' }}</td>
                  <td class="px-4 py-2 font-mono text-xs text-muted">{{ loan.dueDate | date: 'dd/MM/yyyy' }}</td>
                  <td class="px-4 py-2">
                    @if (loan.returned) {
                      <span class="rounded-full border border-line bg-paper px-2 py-0.5 text-xs font-semibold uppercase tracking-wide text-muted">Devuelto</span>
                    } @else if (loan.overdue) {
                      <span class="rounded-full bg-[#f6e7e3] px-2 py-0.5 text-xs font-semibold uppercase tracking-wide text-out">Vencido</span>
                    } @else {
                      <span class="rounded-full bg-[#e7efe7] px-2 py-0.5 text-xs font-semibold uppercase tracking-wide text-ok">Vigente</span>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
})
export class MyLoans implements OnInit {
  private readonly loansService = inject(LoansService);

  protected readonly loans = signal<Loan[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.loansService.mine().subscribe({
      next: (loans) => {
        this.loans.set(loans);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(e, 'No se pudieron cargar tus préstamos.'));
      },
    });
  }
}
