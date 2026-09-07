import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from '../../core/api';
import { Loan } from '../../core/models';

export interface CreateLoanPayload {
  bookId: number;
  borrowerName?: string;
  borrowerEmail?: string;
}

@Injectable({ providedIn: 'root' })
export class LoansService {
  private readonly http = inject(HttpClient);

  mine(): Observable<Loan[]> {
    return this.http.get<Loan[]>(`${API_BASE}/loans/mine`);
  }

  create(payload: CreateLoanPayload): Observable<Loan> {
    return this.http.post<Loan>(`${API_BASE}/loans`, payload);
  }

  return(id: number): Observable<Loan> {
    return this.http.put<Loan>(`${API_BASE}/loans/${id}/return`, {});
  }
}
