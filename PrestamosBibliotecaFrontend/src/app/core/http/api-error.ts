import { HttpErrorResponse } from '@angular/common/http';

/** Extrae el mensaje del cuerpo ApiError del backend, con un fallback amigable. */
export function apiErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    const message = (error.error as { message?: string }).message;
    if (message) {
      return message;
    }
  }
  return fallback;
}
