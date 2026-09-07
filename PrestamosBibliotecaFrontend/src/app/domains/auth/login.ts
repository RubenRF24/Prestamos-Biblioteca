import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="mx-auto mt-10 max-w-sm rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h1 class="mb-4 text-xl font-bold">Iniciar sesión</h1>
      <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4">
        <div>
          <label class="mb-1 block text-sm font-medium" for="email">Correo</label>
          <input
            id="email"
            type="email"
            formControlName="email"
            class="w-full rounded-md border border-slate-300 px-3 py-2"
          />
          @if (form.controls.email.touched && form.controls.email.invalid) {
            <p class="mt-1 text-xs text-red-600">Ingresá un correo válido.</p>
          }
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium" for="password">Contraseña</label>
          <input
            id="password"
            type="password"
            formControlName="password"
            class="w-full rounded-md border border-slate-300 px-3 py-2"
          />
        </div>
        @if (error()) {
          <p class="rounded-md bg-red-50 p-2 text-sm text-red-700">{{ error() }}</p>
        }
        <button
          type="submit"
          [disabled]="loading()"
          class="w-full rounded-md bg-blue-600 py-2 font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {{ loading() ? 'Ingresando…' : 'Ingresar' }}
        </button>
      </form>
      <p class="mt-4 text-center text-sm text-slate-600">
        ¿No tenés cuenta? <a routerLink="/register" class="text-blue-700">Registrate</a>
      </p>
    </div>
  `,
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => this.router.navigate(['/catalog']),
      error: (e) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(e, 'No se pudo iniciar sesión.'));
      },
    });
  }
}
