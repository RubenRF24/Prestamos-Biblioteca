import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from '../../core/api';
import { AuthUser, BlockedUser, Stats } from '../../core/models';

export interface CreateUserPayload {
  name: string;
  email: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  stats(): Observable<Stats> {
    return this.http.get<Stats>(`${API_BASE}/admin/stats`);
  }

  createLibrarian(payload: CreateUserPayload): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${API_BASE}/admin/users`, payload);
  }

  blockedUsers(): Observable<BlockedUser[]> {
    return this.http.get<BlockedUser[]>(`${API_BASE}/admin/users/blocked`);
  }

  unblock(id: number): Observable<unknown> {
    return this.http.put(`${API_BASE}/admin/users/${id}/unblock`, {});
  }
}
