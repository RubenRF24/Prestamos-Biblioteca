import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';

@Component({
  selector: 'app-activate',
  imports: [ReactiveFormsModule],
  template: `
    <div class="mx-auto mt-10 max-w-sm rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h1 class="mb-2 text-xl font-bold">Activá tu cuenta</h1>
      <p class="mb-4 text-sm text-slate-600">Definí una contraseña para empezar a usar tu cuenta.</p>
      @if (!token()) {
        <p class="rounded-md bg-red-50 p-2 text-sm text-red-700">
          Falta el token de activación en el enlace.
        </p>
      } @else {
        <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4">
          <div>
            <label class="mb-1 block text-sm font-medium" for="password">Nueva contraseña</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              class="w-full rounded-md border border-slate-300 px-3 py-2"
            />
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <p class="mt-1 text-xs text-red-600">Mínimo 8 caracteres.</p>
            }
          </div>
          @if (error()) {
            <p class="rounded-md bg-red-50 p-2 text-sm text-red-700">{{ error() }}</p>
          }
          <button
            type="submit"
            [disabled]="loading()"
            class="w-full rounded-md bg-blue-600 py-2 font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {{ loading() ? 'Activando…' : 'Activar cuenta' }}
          </button>
        </form>
      }
    </div>
  `,
})
export class Activate {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly token = signal<string | null>(this.route.snapshot.queryParamMap.get('token'));
  protected readonly form = this.fb.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  submit(): void {
    const token = this.token();
    if (!token || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.activate(token, this.form.getRawValue().password).subscribe({
      next: () => this.router.navigate(['/catalog']),
      error: (e) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(e, 'No se pudo activar la cuenta.'));
      },
    });
  }
}
