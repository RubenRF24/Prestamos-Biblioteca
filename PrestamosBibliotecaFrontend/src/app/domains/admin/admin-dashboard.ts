import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { apiErrorMessage } from '../../core/http/api-error';
import { BlockedUser, Stats } from '../../core/models';
import { AdminService } from './admin.service';

@Component({
  selector: 'app-admin-dashboard',
  imports: [DatePipe],
  template: `
    <div class="space-y-6">
      <h1 class="text-2xl font-bold">Administración</h1>

      @if (error()) {
        <p class="rounded-md bg-red-50 p-2 text-sm text-red-700">{{ error() }}</p>
      }
      @if (message()) {
        <p class="rounded-md bg-emerald-50 p-2 text-sm text-emerald-700">{{ message() }}</p>
      }

      <!-- Estadísticas -->
      @if (stats(); as s) {
        <div class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          <div class="rounded-xl border border-slate-200 bg-white p-4 text-center">
            <p class="text-2xl font-bold">{{ s.totalBooks }}</p>
            <p class="text-xs text-slate-500">Libros</p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-white p-4 text-center">
            <p class="text-2xl font-bold text-emerald-600">{{ s.available }}</p>
            <p class="text-xs text-slate-500">Disponibles</p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-white p-4 text-center">
            <p class="text-2xl font-bold text-red-600">{{ s.borrowed }}</p>
            <p class="text-xs text-slate-500">Prestados</p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-white p-4 text-center">
            <p class="text-2xl font-bold text-amber-600">{{ s.reserved }}</p>
            <p class="text-xs text-slate-500">Reservados</p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-white p-4 text-center">
            <p class="text-2xl font-bold text-red-700">{{ s.overdueLoans }}</p>
            <p class="text-xs text-slate-500">Vencidos</p>
          </div>
          <div class="rounded-xl border border-slate-200 bg-white p-4 text-center">
            <p class="text-2xl font-bold text-slate-700">{{ s.blockedUsers }}</p>
            <p class="text-xs text-slate-500">Bloqueadas</p>
          </div>
        </div>
      }

      <!-- Cuentas bloqueadas -->
      <section class="rounded-xl border border-slate-200 bg-white">
        <h2 class="border-b border-slate-100 px-4 py-3 font-semibold">Cuentas bloqueadas</h2>
        @if (blocked().length === 0) {
          <p class="px-4 py-3 text-sm text-slate-500">No hay cuentas bloqueadas.</p>
        } @else {
          <table class="w-full text-left text-sm">
            <thead class="bg-slate-50 text-slate-600">
              <tr>
                <th class="px-4 py-2">Nombre</th>
                <th class="px-4 py-2">Correo</th>
                <th class="px-4 py-2">Bloqueada hasta</th>
                <th class="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody>
              @for (user of blocked(); track user.id) {
                <tr class="border-t border-slate-100">
                  <td class="px-4 py-2 font-medium">{{ user.name }}</td>
                  <td class="px-4 py-2">{{ user.email }}</td>
                  <td class="px-4 py-2">{{ user.blockedUntil | date: 'dd/MM/yyyy HH:mm' }}</td>
                  <td class="px-4 py-2 text-right">
                    <button
                      type="button"
                      (click)="unblock(user)"
                      class="rounded-md bg-blue-600 px-3 py-1 text-xs font-medium text-white hover:bg-blue-700"
                    >
                      Desbloquear
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </section>
    </div>
  `,
})
export class AdminDashboard implements OnInit {
  private readonly adminService = inject(AdminService);

  protected readonly stats = signal<Stats | null>(null);
  protected readonly blocked = signal<BlockedUser[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.adminService.stats().subscribe({
      next: (stats) => this.stats.set(stats),
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudieron cargar las estadísticas.')),
    });
    this.adminService.blockedUsers().subscribe({
      next: (users) => this.blocked.set(users),
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudieron cargar las cuentas bloqueadas.')),
    });
  }

  unblock(user: BlockedUser): void {
    this.adminService.unblock(user.id).subscribe({
      next: () => {
        this.message.set(`Cuenta desbloqueada: ${user.email}.`);
        this.error.set(null);
        this.load();
      },
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudo desbloquear la cuenta.')),
    });
  }
}
