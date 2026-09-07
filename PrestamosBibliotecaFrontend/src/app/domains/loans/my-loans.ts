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
      <h1 class="text-2xl font-bold">Mis préstamos</h1>

      @if (error()) {
        <p class="rounded-md bg-red-50 p-2 text-sm text-red-700">{{ error() }}</p>
      }
      @if (message()) {
        <p class="rounded-md bg-emerald-50 p-2 text-sm text-emerald-700">{{ message() }}</p>
      }

      @if (loading()) {
        <p class="text-slate-500">Cargando…</p>
      } @else if (loans().length === 0) {
        <p class="text-slate-500">Todavía no tenés préstamos.</p>
      } @else {
        <div class="overflow-x-auto rounded-xl border border-slate-200 bg-white">
          <table class="w-full text-left text-sm">
            <thead class="bg-slate-50 text-slate-600">
              <tr>
                <th class="px-4 py-2">Libro</th>
                <th class="px-4 py-2">Prestado</th>
                <th class="px-4 py-2">Vence</th>
                <th class="px-4 py-2">Estado</th>
                <th class="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody>
              @for (loan of loans(); track loan.id) {
                <tr class="border-t border-slate-100">
                  <td class="px-4 py-2 font-medium">{{ loan.bookTitle }}</td>
                  <td class="px-4 py-2">{{ loan.loanDate | date: 'dd/MM/yyyy' }}</td>
                  <td class="px-4 py-2">{{ loan.dueDate | date: 'dd/MM/yyyy' }}</td>
                  <td class="px-4 py-2">
                    @if (loan.returned) {
                      <span class="rounded-full bg-slate-100 px-2 py-0.5 text-xs">Devuelto</span>
                    } @else if (loan.overdue) {
                      <span class="rounded-full bg-red-100 px-2 py-0.5 text-xs text-red-700">Vencido</span>
                    } @else {
                      <span class="rounded-full bg-emerald-100 px-2 py-0.5 text-xs text-emerald-700">Vigente</span>
                    }
                  </td>
                  <td class="px-4 py-2 text-right">
                    @if (!loan.returned) {
                      <button
                        type="button"
                        (click)="returnLoan(loan)"
                        class="rounded-md bg-blue-600 px-3 py-1 text-xs font-medium text-white hover:bg-blue-700"
                      >
                        Devolver
                      </button>
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
  protected readonly message = signal<string | null>(null);

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

  returnLoan(loan: Loan): void {
    this.loansService.return(loan.id).subscribe({
      next: () => {
        this.message.set(`Devolviste: ${loan.bookTitle}.`);
        this.error.set(null);
        this.load();
      },
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudo devolver el libro.')),
    });
  }
}
