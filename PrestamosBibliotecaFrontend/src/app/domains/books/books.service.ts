import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE } from '../../core/api';
import { Book, BookPreview, BookStatus } from '../../core/models';

export interface CreateBookPayload {
  title: string;
  author: string;
  isbn: string;
  publishedYear: number | null;
}

@Injectable({ providedIn: 'root' })
export class BooksService {
  private readonly http = inject(HttpClient);

  search(query: string, status: BookStatus | ''): Observable<Book[]> {
    let params = new HttpParams();
    if (query) {
      params = params.set('q', query);
    }
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Book[]>(`${API_BASE}/books`, { params });
  }

  lookupByIsbn(isbn: string): Observable<BookPreview> {
    return this.http.get<BookPreview>(`${API_BASE}/books/lookup/${encodeURIComponent(isbn)}`);
  }

  create(payload: CreateBookPayload): Observable<Book> {
    return this.http.post<Book>(`${API_BASE}/books`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/books/${id}`);
  }
}
