import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="mx-auto mt-10 max-w-sm rounded border border-line bg-surface p-6 shadow-sm">
      <h1 class="font-serif mb-4 text-xl font-bold text-ink">Iniciar sesión</h1>
      <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4">
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
        </div>
        @if (error()) {
          <p class="rounded bg-[#f6e7e3] p-2 text-sm text-out">{{ error() }}</p>
        }
        <button
          type="submit"
          [disabled]="loading()"
          class="w-full rounded bg-forest py-2 font-medium text-white hover:bg-forest/90 disabled:opacity-50"
        >
          {{ loading() ? 'Ingresando…' : 'Ingresar' }}
        </button>
      </form>
      <p class="mt-4 text-center text-sm text-muted">
        ¿No tenés cuenta? <a routerLink="/register" class="text-forest hover:underline">Registrate</a>
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
