export type Role = 'ADMIN' | 'BIBLIOTECARIO';
export type BookStatus = 'DISPONIBLE' | 'PRESTADO' | 'RESERVADO';

export interface AuthUser {
  id: number;
  name: string;
  email: string;
  role: Role;
}

export interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  publishedYear: number | null;
  status: BookStatus;
  coverUrl: string | null;
  subjects: string[];
}

export interface BookPreview {
  title: string | null;
  author: string | null;
  publishedYear: number | null;
  coverUrl: string | null;
  subjects: string[];
}

export interface Loan {
  id: number;
  bookId: number;
  bookTitle: string;
  borrowerName: string;
  borrowerEmail: string;
  loanDate: string;
  dueDate: string;
  returnDate: string | null;
  returned: boolean;
  overdue: boolean;
}

export interface Reservation {
  id: number;
  bookId: number;
  bookTitle: string;
  requesterEmail: string;
  status: string;
  requestedAt: string;
  expiresAt: string | null;
}

export interface Stats {
  totalBooks: number;
  available: number;
  borrowed: number;
  reserved: number;
  overdueLoans: number;
  blockedUsers: number;
}

export interface BlockedUser {
  id: number;
  name: string;
  email: string;
  blockedUntil: string | null;
}

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
}
