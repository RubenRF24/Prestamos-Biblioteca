import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from '../../core/api';
import { BlockedUser, Stats } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  stats(): Observable<Stats> {
    return this.http.get<Stats>(`${API_BASE}/admin/stats`);
  }

  blockedUsers(): Observable<BlockedUser[]> {
    return this.http.get<BlockedUser[]>(`${API_BASE}/admin/users/blocked`);
  }

  unblock(id: number): Observable<unknown> {
    return this.http.put(`${API_BASE}/admin/users/${id}/unblock`, {});
  }
}
