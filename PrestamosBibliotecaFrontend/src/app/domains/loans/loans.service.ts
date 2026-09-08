import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from '../../core/api';
import { Loan, Page } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class LoansService {
  private readonly http = inject(HttpClient);

  /** Préstamos del lector logueado (USUARIO). */
  mine(): Observable<Loan[]> {
    return this.http.get<Loan[]>(`${API_BASE}/loans/mine`);
  }

  /** Préstamos activos paginados (BIBLIOTECARIO): quién tiene cada libro. */
  active(page: number, size: number): Observable<Page<Loan>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Loan>>(`${API_BASE}/loans/active`, { params });
  }

  /** Préstamo directo a un tercero (BIBLIOTECARIO): crea la cuenta USUARIO si no existe. */
  create(payload: { bookId: number; borrowerName: string; borrowerEmail: string }): Observable<Loan> {
    return this.http.post<Loan>(`${API_BASE}/loans`, payload);
  }

  /** El bibliotecario confirma una reserva retenida y arranca el préstamo. */
  confirm(reservationId: number): Observable<Loan> {
    return this.http.post<Loan>(`${API_BASE}/loans/confirm/${reservationId}`, {});
  }

  return(id: number): Observable<Loan> {
    return this.http.put<Loan>(`${API_BASE}/loans/${id}/return`, {});
  }
}
