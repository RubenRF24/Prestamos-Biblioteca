import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from '../../core/api';
import { Reservation } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class ReservationsService {
  private readonly http = inject(HttpClient);

  create(bookId: number): Observable<Reservation> {
    return this.http.post<Reservation>(`${API_BASE}/reservations`, { bookId });
  }

  cancel(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/reservations/${id}`);
  }
}
