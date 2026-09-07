import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthStore } from './core/auth/auth.store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-dvh bg-slate-50 text-slate-800">
      <header class="border-b border-slate-200 bg-white">
        <nav class="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <a routerLink="/catalog" class="text-lg font-bold text-blue-700">📚 Biblioteca</a>
          @if (auth.isAuthenticated()) {
            <div class="flex items-center gap-4 text-sm">
              <a
                routerLink="/catalog"
                routerLinkActive="text-blue-700 font-semibold"
                class="hover:text-blue-700"
                >Catálogo</a
              >
              <a
                routerLink="/my-loans"
                routerLinkActive="text-blue-700 font-semibold"
                class="hover:text-blue-700"
                >Mis préstamos</a
              >
              @if (auth.isAdmin()) {
                <a
                  routerLink="/admin"
                  routerLinkActive="text-blue-700 font-semibold"
                  class="hover:text-blue-700"
                  >Administración</a
                >
              }
              <span class="text-slate-400">|</span>
              <span class="text-slate-600">{{ auth.user()?.name }}</span>
              <button
                type="button"
                (click)="auth.logout()"
                class="rounded-md bg-slate-100 px-3 py-1 font-medium hover:bg-slate-200"
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
