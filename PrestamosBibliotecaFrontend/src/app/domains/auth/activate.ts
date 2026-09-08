import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';

@Component({
  selector: 'app-activate',
  imports: [ReactiveFormsModule],
  template: `
    <div class="mx-auto mt-10 max-w-sm rounded border border-line bg-surface p-6 shadow-sm">
      <h1 class="font-serif mb-2 text-xl font-bold text-ink">Activá tu cuenta</h1>
      <p class="mb-4 text-sm text-muted">Definí una contraseña para empezar a usar tu cuenta.</p>
      @if (!token()) {
        <p class="rounded bg-[#f6e7e3] p-2 text-sm text-out">
          Falta el token de activación en el enlace.
        </p>
      } @else {
        <form [formGroup]="form" (ngSubmit)="submit()" class="space-y-4">
          <div>
            <label class="mb-1 block text-sm font-medium text-ink" for="password">Nueva contraseña</label>
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
