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
