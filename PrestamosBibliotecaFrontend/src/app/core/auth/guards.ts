import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthStore } from './auth.store';

/** Requiere sesión. Si aún no se hidrató, consulta /me primero. */
export const authGuard: CanActivateFn = () => {
  const store = inject(AuthStore);
  const router = inject(Router);
  if (store.initialized()) {
    return store.isAuthenticated() ? true : router.parseUrl('/login');
  }
  return store.loadCurrentUser().pipe(map((user) => (user ? true : router.parseUrl('/login'))));
};

/** Sólo el lector (USUARIO): sus préstamos y reservas. */
export const usuarioGuard: CanActivateFn = () => roleGuard((s) => s.isUsuario());

/** Sólo el personal de mesa (BIBLIOTECARIO): confirmar y ver préstamos activos. */
export const bibliotecarioGuard: CanActivateFn = () => roleGuard((s) => s.isBibliotecario());

function roleGuard(allowed: (store: AuthStore) => boolean) {
  const store = inject(AuthStore);
  const router = inject(Router);
  const check = () => (allowed(store) ? true : router.parseUrl('/catalog'));
  if (store.initialized()) {
    return check();
  }
  return store.loadCurrentUser().pipe(map(() => check()));
}

/** Requiere rol ADMIN. */
export const adminGuard: CanActivateFn = () => {
  const store = inject(AuthStore);
  const router = inject(Router);
  const check = () => (store.isAdmin() ? true : router.parseUrl('/catalog'));
  if (store.initialized()) {
    return check();
  }
  return store.loadCurrentUser().pipe(map(() => check()));
};
