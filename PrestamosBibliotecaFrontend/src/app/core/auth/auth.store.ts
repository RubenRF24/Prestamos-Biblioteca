import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, of, tap } from 'rxjs';
import { API_BASE } from '../api';
import { AuthUser } from '../models';

/** Estado de sesión con signals. La cookie es httpOnly, así que la sesión se hidrata desde /me. */
@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _user = signal<AuthUser | null>(null);
  private readonly _initialized = signal(false);

  readonly user = this._user.asReadonly();
  readonly initialized = this._initialized.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly isAdmin = computed(() => this._user()?.role === 'ADMIN');

  /** Consulta al backend quién es el usuario actual (200 = logueado, 401 = anónimo). */
  loadCurrentUser(): Observable<AuthUser | null> {
    return this.http.get<AuthUser>(`${API_BASE}/auth/me`).pipe(
      tap((user) => this._user.set(user)),
      catchError(() => {
        this._user.set(null);
        return of(null);
      }),
      finalize(() => this._initialized.set(true)),
    );
  }

  login(email: string, password: string): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${API_BASE}/auth/login`, { email, password })
      .pipe(tap((user) => this._user.set(user)));
  }

  register(name: string, email: string, password: string): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${API_BASE}/auth/register`, { name, email, password })
      .pipe(tap((user) => this._user.set(user)));
  }

  activate(token: string, password: string): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${API_BASE}/auth/activate`, { token, password })
      .pipe(tap((user) => this._user.set(user)));
  }

  logout(): void {
    this.http.post(`${API_BASE}/auth/logout`, {}).subscribe({
      next: () => this.clearAndRedirect(),
      error: () => this.clearAndRedirect(),
    });
  }

  clearSession(): void {
    this._user.set(null);
  }

  private clearAndRedirect(): void {
    this._user.set(null);
    this.router.navigate(['/login']);
  }
}
