import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';
import { Book, BookStatus } from '../../core/models';
import { LoansService } from '../loans/loans.service';
import { ReservationsService } from '../reservations/reservations.service';
import { BooksService } from './books.service';

@Component({
  selector: 'app-catalog',
  imports: [ReactiveFormsModule],
  template: `
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-bold">Catálogo</h1>
      </div>

      <!-- Búsqueda -->
      <form [formGroup]="searchForm" (ngSubmit)="load()" class="flex flex-wrap gap-3">
        <input
          type="text"
          formControlName="q"
          placeholder="Buscar por título o autor…"
          class="flex-1 rounded-md border border-slate-300 px-3 py-2"
        />
        <select formControlName="status" class="rounded-md border border-slate-300 px-3 py-2">
          <option value="">Todos los estados</option>
          <option value="DISPONIBLE">Disponible</option>
          <option value="PRESTADO">Prestado</option>
          <option value="RESERVADO">Reservado</option>
        </select>
        <button type="submit" class="rounded-md bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700">
          Buscar
        </button>
      </form>

      @if (message()) {
        <p class="rounded-md bg-emerald-50 p-2 text-sm text-emerald-700">{{ message() }}</p>
      }
      @if (error()) {
        <p class="rounded-md bg-red-50 p-2 text-sm text-red-700">{{ error() }}</p>
      }

      <!-- Alta de libro (solo ADMIN) -->
      @if (auth.isAdmin()) {
        <section class="rounded-xl border border-slate-200 bg-white p-4">
          <h2 class="mb-3 font-semibold">Agregar libro</h2>
          <form [formGroup]="createForm" (ngSubmit)="create()" class="grid gap-3 md:grid-cols-2">
            <div class="md:col-span-2 flex gap-2">
              <input
                type="text"
                formControlName="isbn"
                placeholder="ISBN"
                class="flex-1 rounded-md border border-slate-300 px-3 py-2"
              />
              <button
                type="button"
                (click)="lookup()"
                [disabled]="lookupLoading()"
                class="rounded-md bg-slate-100 px-3 py-2 text-sm font-medium hover:bg-slate-200 disabled:opacity-50"
              >
                {{ lookupLoading() ? 'Buscando…' : 'Autocompletar desde ISBN' }}
              </button>
            </div>
            <input
              type="text"
              formControlName="title"
              placeholder="Título"
              class="rounded-md border border-slate-300 px-3 py-2"
            />
            <input
              type="text"
              formControlName="author"
              placeholder="Autor"
              class="rounded-md border border-slate-300 px-3 py-2"
            />
            <input
              type="number"
              formControlName="publishedYear"
              placeholder="Año"
              class="rounded-md border border-slate-300 px-3 py-2"
            />
            <div class="md:col-span-2">
              <button
                type="submit"
                [disabled]="createLoading()"
                class="rounded-md bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
              >
                {{ createLoading() ? 'Guardando…' : 'Guardar libro' }}
              </button>
            </div>
          </form>
        </section>
      }

      <!-- Listado -->
      @if (loading()) {
        <p class="text-slate-500">Cargando catálogo…</p>
      } @else if (books().length === 0) {
        <p class="text-slate-500">No se encontraron libros.</p>
      } @else {
        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          @for (book of books(); track book.id) {
            <article class="flex flex-col rounded-xl border border-slate-200 bg-white p-4">
              <div class="flex gap-3">
                @if (book.coverUrl) {
                  <img
                    [src]="book.coverUrl"
                    [alt]="'Portada de ' + book.title"
                    class="h-24 w-16 rounded object-cover"
                  />
                }
                <div class="min-w-0 flex-1">
                  <h3 class="truncate font-semibold" [title]="book.title">{{ book.title }}</h3>
                  <p class="truncate text-sm text-slate-600">{{ book.author }}</p>
                  @if (book.publishedYear) {
                    <p class="text-xs text-slate-400">{{ book.publishedYear }}</p>
                  }
                  <span
                    class="mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-medium"
                    [class]="statusClass(book.status)"
                    >{{ book.status }}</span
                  >
                </div>
              </div>
              <div class="mt-3 flex gap-2">
                @if (book.status === 'DISPONIBLE') {
                  <button
                    type="button"
                    (click)="lend(book)"
                    class="rounded-md bg-blue-600 px-3 py-1 text-sm font-medium text-white hover:bg-blue-700"
                  >
                    Prestar
                  </button>
                } @else {
                  <button
                    type="button"
                    (click)="reserve(book)"
                    class="rounded-md bg-amber-500 px-3 py-1 text-sm font-medium text-white hover:bg-amber-600"
                  >
                    Reservar
                  </button>
                }
                @if (auth.isAdmin() && book.status === 'DISPONIBLE') {
                  <button
                    type="button"
                    (click)="remove(book)"
                    class="rounded-md bg-red-100 px-3 py-1 text-sm font-medium text-red-700 hover:bg-red-200"
                  >
                    Eliminar
                  </button>
                }
              </div>
            </article>
          }
        </div>
      }
    </div>
  `,
})
export class Catalog implements OnInit {
  protected readonly auth = inject(AuthStore);
  private readonly fb = inject(FormBuilder);
  private readonly booksService = inject(BooksService);
  private readonly loansService = inject(LoansService);
  private readonly reservationsService = inject(ReservationsService);

  protected readonly books = signal<Book[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly lookupLoading = signal(false);
  protected readonly createLoading = signal(false);

  protected readonly searchForm = this.fb.nonNullable.group({
    q: [''],
    status: [''],
  });
  protected readonly createForm = this.fb.nonNullable.group({
    title: ['', [Validators.required]],
    author: ['', [Validators.required]],
    isbn: ['', [Validators.required]],
    publishedYear: [null as number | null],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const { q, status } = this.searchForm.getRawValue();
    this.booksService.search(q, status as BookStatus | '').subscribe({
      next: (books) => {
        this.books.set(books);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(e, 'No se pudo cargar el catálogo.'));
      },
    });
  }

  lookup(): void {
    const isbn = this.createForm.getRawValue().isbn;
    if (!isbn) {
      return;
    }
    this.lookupLoading.set(true);
    this.error.set(null);
    this.booksService.lookupByIsbn(isbn).subscribe({
      next: (preview) => {
        this.lookupLoading.set(false);
        this.createForm.patchValue({
          title: preview.title ?? this.createForm.getRawValue().title,
          author: preview.author ?? this.createForm.getRawValue().author,
          publishedYear: preview.publishedYear ?? this.createForm.getRawValue().publishedYear,
        });
      },
      error: () => {
        this.lookupLoading.set(false);
        this.error.set('No se encontró el ISBN en Open Library. Completá los datos a mano.');
      },
    });
  }

  create(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.createLoading.set(true);
    this.flash(null);
    this.booksService.create(this.createForm.getRawValue()).subscribe({
      next: () => {
        this.createLoading.set(false);
        this.createForm.reset({ title: '', author: '', isbn: '', publishedYear: null });
        this.flash('Libro agregado.');
        this.load();
      },
      error: (e) => {
        this.createLoading.set(false);
        this.error.set(apiErrorMessage(e, 'No se pudo agregar el libro.'));
      },
    });
  }

  lend(book: Book): void {
    this.loansService.create({ bookId: book.id }).subscribe({
      next: () => {
        this.flash(`Préstamo registrado: ${book.title}.`);
        this.load();
      },
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudo registrar el préstamo.')),
    });
  }

  reserve(book: Book): void {
    this.reservationsService.create(book.id).subscribe({
      next: () => this.flash(`Quedaste en la lista de espera de: ${book.title}.`),
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudo reservar.')),
    });
  }

  remove(book: Book): void {
    this.booksService.delete(book.id).subscribe({
      next: () => {
        this.flash('Libro eliminado.');
        this.load();
      },
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudo eliminar el libro.')),
    });
  }

  statusClass(status: BookStatus): string {
    switch (status) {
      case 'DISPONIBLE':
        return 'bg-emerald-100 text-emerald-700';
      case 'PRESTADO':
        return 'bg-red-100 text-red-700';
      default:
        return 'bg-amber-100 text-amber-700';
    }
  }

  private flash(text: string | null): void {
    this.message.set(text);
    this.error.set(null);
  }
}
