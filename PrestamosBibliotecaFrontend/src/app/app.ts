import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthStore } from './core/auth/auth.store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-dvh bg-paper text-ink">
      <header class="border-b border-line bg-surface">
        <nav class="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <a routerLink="/catalog" class="font-serif text-lg font-bold tracking-tight text-forest">📚 Biblioteca</a>
          @if (auth.isAuthenticated()) {
            <div class="flex items-center gap-4 text-sm">
              <a
                routerLink="/catalog"
                routerLinkActive="text-forest font-semibold shadow-[inset_0_-2px_0_var(--color-amber)]"
                class="hover:text-forest"
                >Catálogo</a
              >
              @if (auth.isUsuario()) {
                <a
                  routerLink="/my-loans"
                  routerLinkActive="text-forest font-semibold shadow-[inset_0_-2px_0_var(--color-amber)]"
                  class="hover:text-forest"
                  >Mis préstamos</a
                >
              }
              @if (auth.isBibliotecario()) {
                <a
                  routerLink="/active-loans"
                  routerLinkActive="text-forest font-semibold shadow-[inset_0_-2px_0_var(--color-amber)]"
                  class="hover:text-forest"
                  >Préstamos activos</a
                >
              }
              @if (auth.isAdmin()) {
                <a
                  routerLink="/admin"
                  routerLinkActive="text-forest font-semibold shadow-[inset_0_-2px_0_var(--color-amber)]"
                  class="hover:text-forest"
                  >Administración</a
                >
              }
              <span class="text-muted">|</span>
              <span class="text-muted">{{ auth.user()?.name }}</span>
              <button
                type="button"
                (click)="auth.logout()"
                class="rounded border border-line bg-paper px-3 py-1 font-medium text-ink hover:bg-surface"
              >
                Salir
              </button>
            </div>
          }
        </nav>
      </header>
      <main class="mx-auto max-w-5xl px-4 py-6">
        <router-outlet />
      </main>
    </div>
  `,
})
export class App implements OnInit {
  protected readonly auth = inject(AuthStore);

  ngOnInit(): void {
    if (!this.auth.initialized()) {
      this.auth.loadCurrentUser().subscribe();
    }
  }
}
