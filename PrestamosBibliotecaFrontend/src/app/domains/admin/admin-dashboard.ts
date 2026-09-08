import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { apiErrorMessage } from '../../core/http/api-error';
import { BlockedUser, Stats } from '../../core/models';
import { AdminService } from './admin.service';

@Component({
  selector: 'app-admin-dashboard',
  imports: [DatePipe, ReactiveFormsModule],
  template: `
    <div class="space-y-6">
      <h1 class="font-serif text-2xl font-bold text-ink">Administración</h1>

      @if (error()) {
        <p class="rounded bg-[#f6e7e3] p-2 text-sm text-out">{{ error() }}</p>
      }
      @if (message()) {
        <p class="rounded bg-[#e7efe7] p-2 text-sm text-ok">{{ message() }}</p>
      }

      <!-- Estadísticas -->
      @if (stats(); as s) {
        <div class="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          <div class="rounded border border-line bg-surface p-4 text-center">
            <p class="font-serif text-2xl font-bold text-ink">{{ s.totalBooks }}</p>
            <p class="text-xs text-muted">Libros</p>
          </div>
          <div class="rounded border border-line bg-surface p-4 text-center">
            <p class="font-serif text-2xl font-bold text-ok">{{ s.available }}</p>
            <p class="text-xs text-muted">Disponibles</p>
          </div>
          <div class="rounded border border-line bg-surface p-4 text-center">
            <p class="font-serif text-2xl font-bold text-out">{{ s.borrowed }}</p>
            <p class="text-xs text-muted">Prestados</p>
          </div>
          <div class="rounded border border-line bg-surface p-4 text-center">
            <p class="font-serif text-2xl font-bold text-res">{{ s.reserved }}</p>
            <p class="text-xs text-muted">Reservados</p>
          </div>
          <div class="rounded border border-line bg-surface p-4 text-center">
            <p class="font-serif text-2xl font-bold text-out">{{ s.overdueLoans }}</p>
            <p class="text-xs text-muted">Vencidos</p>
          </div>
          <div class="rounded border border-line bg-surface p-4 text-center">
            <p class="font-serif text-2xl font-bold text-ink">{{ s.blockedUsers }}</p>
            <p class="text-xs text-muted">Bloqueadas</p>
          </div>
        </div>
      }

      <!-- Registrar bibliotecario -->
      <section class="rounded border border-line bg-surface p-4">
        <h2 class="font-serif mb-1 font-semibold text-ink">Registrar bibliotecario</h2>
        <p class="mb-3 text-sm text-muted">
          Da de alta una cuenta de personal (rol BIBLIOTECARIO). Recibirá un correo de bienvenida.
        </p>
        <form [formGroup]="librarianForm" (ngSubmit)="createLibrarian()" class="grid gap-3 md:grid-cols-3">
          <input
            type="text"
            formControlName="name"
            placeholder="Nombre"
            class="rounded border border-line bg-surface px-3 py-2 text-ink placeholder:text-muted focus:outline-none focus:border-forest"
          />
          <input
            type="email"
            formControlName="email"
            placeholder="Correo"
            class="rounded border border-line bg-surface px-3 py-2 text-ink placeholder:text-muted focus:outline-none focus:border-forest"
          />
          <input
            type="password"
            formControlName="password"
            placeholder="Contraseña (mín. 8)"
            class="rounded border border-line bg-surface px-3 py-2 text-ink placeholder:text-muted focus:outline-none focus:border-forest"
          />
          <div class="md:col-span-3">
            <button
              type="submit"
              [disabled]="librarianLoading()"
              class="rounded bg-forest px-4 py-2 font-medium text-white hover:bg-forest/90 disabled:opacity-50"
            >
              {{ librarianLoading() ? 'Creando…' : 'Registrar bibliotecario' }}
            </button>
          </div>
        </form>
      </section>

      <!-- Cuentas bloqueadas -->
      <section class="rounded border border-line bg-surface">
        <h2 class="font-serif border-b border-line px-4 py-3 font-semibold text-ink">Cuentas bloqueadas</h2>
        @if (blocked().length === 0) {
          <p class="px-4 py-3 text-sm text-muted">No hay cuentas bloqueadas.</p>
        } @else {
          <table class="w-full text-left text-sm">
            <thead class="bg-paper font-serif text-muted">
              <tr>
                <th class="px-4 py-2">Nombre</th>
                <th class="px-4 py-2">Correo</th>
                <th class="px-4 py-2">Bloqueada hasta</th>
                <th class="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody>
              @for (user of blocked(); track user.id) {
                <tr class="border-t border-line">
                  <td class="px-4 py-2 font-medium text-ink">{{ user.name }}</td>
                  <td class="px-4 py-2 text-ink">{{ user.email }}</td>
                  <td class="px-4 py-2 font-mono text-xs text-muted">{{ user.blockedUntil | date: 'dd/MM/yyyy HH:mm' }}</td>
                  <td class="px-4 py-2 text-right">
                    <button
                      type="button"
                      (click)="unblock(user)"
                      class="rounded bg-forest px-3 py-1 text-xs font-medium text-white hover:bg-forest/90"
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
  private readonly fb = inject(FormBuilder);

  protected readonly stats = signal<Stats | null>(null);
  protected readonly blocked = signal<BlockedUser[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly librarianLoading = signal(false);
  protected readonly librarianForm = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    this.load();
  }

  createLibrarian(): void {
    if (this.librarianForm.invalid) {
      this.librarianForm.markAllAsTouched();
      return;
    }
    this.librarianLoading.set(true);
    this.error.set(null);
    this.adminService.createLibrarian(this.librarianForm.getRawValue()).subscribe({
      next: (user) => {
        this.librarianLoading.set(false);
        this.librarianForm.reset({ name: '', email: '', password: '' });
        this.message.set(`Bibliotecario registrado: ${user.email}.`);
      },
      error: (e) => {
        this.librarianLoading.set(false);
        this.error.set(apiErrorMessage(e, 'No se pudo registrar el bibliotecario.'));
      },
    });
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
