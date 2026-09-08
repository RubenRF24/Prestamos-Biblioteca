import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="mx-auto mt-10 max-w-sm rounded border border-line bg-surface p-6 shadow-sm">
      <h1 class="font-serif mb-4 text-xl font-bold text-ink">Crear cuenta</h1>
      <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4">
        <div>
          <label class="mb-1 block text-sm font-medium text-ink" for="name">Nombre</label>
          <input
            id="name"
            type="text"
            formControlName="name"
            class="w-full rounded border border-line bg-surface px-3 py-2 text-ink placeholder:text-muted focus:outline-none focus:border-forest"
          />
          @if (form.controls.name.touched && form.controls.name.invalid) {
            <p class="mt-1 text-xs text-out">El nombre es obligatorio.</p>
          }
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-ink" for="email">Correo</label>
          <input
            id="email"
            type="email"
            formControlName="email"
            class="w-full rounded border border-line bg-surface px-3 py-2 text-ink placeholder:text-muted focus:outline-none focus:border-forest"
          />
          @if (form.controls.email.touched && form.controls.email.invalid) {
            <p class="mt-1 text-xs text-out">Ingresá un correo válido.</p>
          }
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-ink" for="password">Contraseña</label>
          <input
            id="password"
            type="password"
            formControlName="password"
            class="w-full rounded border border-line bg-surface px-3 py-2 text-ink placeholder:text-muted focus:outline-none focus:border-forest"
          />
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <p class="mt-1 text-xs text-out">Mínimo 8 caracteres.</p>
          }
        </div>
        @if (error()) {
          <p class="rounded bg-[#f6e7e3] p-2 text-sm text-out">{{ error() }}</p>
        }
        <button
          type="submit"
          [disabled]="loading()"
          class="w-full rounded bg-forest py-2 font-medium text-white hover:bg-forest/90 disabled:opacity-50"
        >
          {{ loading() ? 'Creando…' : 'Registrarme' }}
        </button>
      </form>
      <p class="mt-4 text-center text-sm text-muted">
        ¿Ya tenés cuenta? <a routerLink="/login" class="text-forest hover:underline">Iniciá sesión</a>
      </p>
    </div>
  `,
})
export class Register {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const { name, email, password } = this.form.getRawValue();
    this.auth.register(name, email, password).subscribe({
      next: () => this.router.navigate(['/catalog']),
      error: (e) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(e, 'No se pudo crear la cuenta.'));
      },
    });
  }
}
