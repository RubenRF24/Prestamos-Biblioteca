import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Envía la cookie httpOnly en cada request al backend (mismo origen). El JWT nunca se
 * toca desde JS: viaja solo en la cookie. Angular agrega el header X-XSRF-TOKEN aparte.
 */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req.clone({ withCredentials: true }));
};
