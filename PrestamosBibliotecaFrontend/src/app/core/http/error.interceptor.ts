import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthStore } from '../auth/auth.store';

/**
 * Ante un 401 en un endpoint que no sea el de sesión, limpia la sesión y manda a login.
 * El resto de los errores se propagan para que cada pantalla los muestre (nada de tragarse un 500).
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  return next(req).pipe(
    catchError((error) => {
      const isAuthProbe = req.url.includes('/auth/me') || req.url.includes('/auth/login');
      if (error.status === 401 && !isAuthProbe) {
        authStore.clearSession();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
