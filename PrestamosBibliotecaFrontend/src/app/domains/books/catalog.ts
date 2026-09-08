import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthStore } from '../../core/auth/auth.store';
import { apiErrorMessage } from '../../core/http/api-error';
import { Book, BookStatus, Reservation } from '../../core/models';
import { LoansService } from '../loans/loans.service';
import { ReservationsService } from '../reservations/reservations.service';
import { BooksService } from './books.service';

@Component({
  selector: 'app-catalog',
  imports: [ReactiveFormsModule],
  template: `
    <div class="space-y-6">
      <h1 class="font-serif text-2xl font-bold text-ink">Catálogo</h1>

      <!-- Búsqueda -->
      <form [formGroup]="searchForm" (ngSubmit)="load()" class="flex flex-wrap gap-3">
        <input
          type="text"
          formControlName="q"
          placeholder="Buscar por título o autor…"
          class="flex-1 rounded border border-line bg-surface px-3 py-2 text-ink placeholder:text-muted focus:outline-none focus:border-forest"
        />
        <select formControlName="status" class="rounded border border-line bg-surface px-3 py-2 text-ink focus:outline-none focus:border-forest">
          <option value="">Todos los estados</option>
          <option value="DISPONIBLE">Disponible</option>
          <option value="PRESTADO">Prestado</option>
          <option value="RESERVADO">Reservado</option>
        </select>
        <button type="submit" class="rounded bg-forest px-4 py-2 font-medium text-white hover:bg-forest/90">
          Buscar
        </button>
      </form>

      @if (message()) {
        <p class="rounded bg-[#e7efe7] p-2 text-sm text-ok">{{ message() }}</p>
      }
      @if (error()) {
        <p class="rounded bg-[#f6e7e3] p-2 text-sm text-out">{{ error() }}</p>
      }

      <!-- Aviso de reservas por confirmar (solo BIBLIOTECARIO) -->
      @if (auth.isBibliotecario()) {
        @if (pending().length > 0) {
          <p class="rounded border border-amber/60 bg-amber/5 p-3 text-sm text-ink">
            Tenés <strong>{{ pending().length }}</strong> reserva(s) por confirmar. Están marcadas en el listado.
          </p>
        } @else {
          <p class="rounded border border-line bg-surface p-3 text-sm text-muted">
            No hay reservas por confirmar en este momento.
          </p>
        }
      }

      <!-- Alta de libro (solo ADMIN) -->
      @if (auth.isAdmin()) {
        <section class="rounded border border-line bg-surface p-4">
          <h2 class="font-serif mb-3 font-semibold text-ink">Agregar libro</h2>
          <form [formGroup]="createForm" (ngSubmit)="create()" class="grid gap-3 md:grid-cols-2">
            <div class="md:col-span-2 flex gap-2">
              <input
                type="text"
                formControlName="isbn"
                placeholder="ISBN"
                class="flex-1 rounded border border-line bg-surface px-3 py-2 font-mono text-xs text-ink placeholder:text-muted focus:outline-none focus:border-forest"
              />
              <button
                type="button"
                (click)="lookup()"
                [disabled]="lookupLoading()"
                class="rounded border border-line bg-transparent px-3 py-2 text-sm font-medium text-ink hover:bg-forest/5 disabled:opacity-50"
              >
                {{ lookupLoading() ? 'Buscando…' : 'Autocompletar desde ISBN' }}
              </button>
            </div>
            <input
              type="text"
              formControlName="title"
              placeholder="Título"
              class="rounded border border-line bg-surface px-3 py-2 text-ink placeholder:text-muted focus:outline-none focus:border-forest"
            />
            <input
              type="text"
              formControlName="author"
              placeholder="Autor"
              class="rounded border border-line bg-surface px-3 py-2 text-ink placeholder:text-muted focus:outline-none focus:border-forest"
            />
            <input
              type="number"
              formControlName="publishedYear"
              placeholder="Año"
              class="rounded border border-line bg-surface px-3 py-2 text-ink placeholder:text-muted focus:outline-none focus:border-forest"
            />
            <div class="md:col-span-2">
              <button
                type="submit"
                [disabled]="createLoading()"
                class="rounded bg-forest px-4 py-2 font-medium text-white hover:bg-forest/90 disabled:opacity-50"
              >
                {{ createLoading() ? 'Guardando…' : 'Guardar libro' }}
              </button>
            </div>
          </form>
        </section>
      }

      <!-- Listado -->
      @if (loading()) {
        <p class="text-muted">Cargando catálogo…</p>
      } @else if (books().length === 0) {
        <p class="text-muted">No se encontraron libros.</p>
      } @else {
        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          @for (book of books(); track book.id) {
            <article class="flex flex-col rounded border border-line bg-surface p-4">
              <div class="flex gap-3">
                @if (book.coverUrl) {
                  <img
                    [src]="book.coverUrl"
                    [alt]="'Portada de ' + book.title"
                    class="h-24 w-16 rounded border border-line object-cover"
                  />
                } @else {
                  <div class="flex h-24 w-16 shrink-0 items-center justify-center rounded bg-forest p-2 text-center">
                    <span class="font-serif text-[10px] font-semibold leading-tight text-[#e9e2cf] line-clamp-4">{{ book.title }}</span>
                  </div>
                }
                <div class="min-w-0 flex-1">
                  <h3 class="font-serif truncate font-semibold text-ink" [title]="book.title">{{ book.title }}</h3>
                  <p class="truncate text-sm text-muted">{{ book.author }}</p>
                  @if (book.publishedYear) {
                    <p class="font-mono text-xs text-muted">{{ book.publishedYear }}</p>
                  }
                  <span
                    class="mt-1 inline-block rounded-full px-2 py-0.5 text-xs font-semibold uppercase tracking-wide"
                    [class]="statusClass(book.status)"
                    >{{ book.status }}</span
                  >
                </div>
              </div>

              @if (editingId() === book.id) {
                <!-- Edición inline (solo ADMIN) -->
                <form [formGroup]="editForm" (ngSubmit)="saveEdit(book)" class="mt-3 space-y-2">
                  <input type="text" formControlName="title" placeholder="Título"
                    class="w-full rounded border border-line bg-surface px-2 py-1 text-sm text-ink placeholder:text-muted focus:outline-none focus:border-forest" />
                  <input type="text" formControlName="author" placeholder="Autor"
                    class="w-full rounded border border-line bg-surface px-2 py-1 text-sm text-ink placeholder:text-muted focus:outline-none focus:border-forest" />
                  <input type="number" formControlName="publishedYear" placeholder="Año"
                    class="w-full rounded border border-line bg-surface px-2 py-1 text-sm text-ink placeholder:text-muted focus:outline-none focus:border-forest" />
                  <div class="flex gap-2">
                    <button type="submit" class="rounded bg-forest px-3 py-1 text-sm font-medium text-white hover:bg-forest/90">Guardar</button>
                    <button type="button" (click)="cancelEdit()" class="rounded border border-line bg-transparent px-3 py-1 text-sm font-medium text-ink hover:bg-forest/5">Cancelar</button>
                  </div>
                </form>
              } @else {
                <div class="mt-3">
                  @if (auth.isAdmin()) {
                    <div class="flex flex-wrap gap-2">
                      <button type="button" (click)="startEdit(book)" class="rounded bg-forest px-3 py-1 text-sm font-medium text-white hover:bg-forest/90">Editar</button>
                      @if (book.status === 'DISPONIBLE') {
                        <button type="button" (click)="remove(book)" class="rounded bg-[#f6e7e3] px-3 py-1 text-sm font-medium text-out hover:bg-[#efd9d3]">Eliminar</button>
                      }
                    </div>
                  } @else if (auth.isBibliotecario()) {
                    <!-- El bibliotecario confirma reservas retenidas o presta directo a un tercero. -->
                    @if (pendingFor(book.id); as res) {
                      <div class="rounded border border-amber/60 bg-amber/5 p-2">
                        <p class="mb-2 text-xs text-muted">
                          Reservado por <span class="font-mono">{{ res.requesterEmail }}</span>
                        </p>
                        <button type="button" (click)="confirm(res)"
                          class="rounded bg-forest px-3 py-1 text-sm font-medium text-white hover:bg-forest/90">
                          Confirmar préstamo
                        </button>
                      </div>
                    } @else if (lendingId() === book.id) {
                      <form [formGroup]="lendForm" (ngSubmit)="submitLend(book)" class="space-y-2">
                        <input type="text" formControlName="borrowerName" placeholder="Nombre de quien recibe"
                          class="w-full rounded border border-line bg-surface px-2 py-1 text-sm text-ink placeholder:text-muted focus:outline-none focus:border-forest" />
                        <input type="email" formControlName="borrowerEmail" placeholder="Email de quien recibe"
                          class="w-full rounded border border-line bg-surface px-2 py-1 text-sm text-ink placeholder:text-muted focus:outline-none focus:border-forest" />
                        <div class="flex gap-2">
                          <button type="submit" class="rounded bg-forest px-3 py-1 text-sm font-medium text-white hover:bg-forest/90">Confirmar préstamo</button>
                          <button type="button" (click)="cancelLend()" class="rounded border border-line bg-transparent px-3 py-1 text-sm font-medium text-ink hover:bg-forest/5">Cancelar</button>
                        </div>
                      </form>
                    } @else if (book.status === 'DISPONIBLE') {
                      <button type="button" (click)="startLend(book)"
                        class="rounded bg-forest px-3 py-1 text-sm font-medium text-white hover:bg-forest/90">
                        Prestar
                      </button>
                    }
                  } @else {
                    <!-- USUARIO: reserva; si ya lo tiene, se indica. -->
                    @if (hasBorrowed(book.id)) {
                      <p class="text-sm italic text-muted">Ya lo tenés prestado</p>
                    } @else if (myReservationFor(book.id); as r) {
                      <div class="flex flex-wrap items-center gap-2">
                        <span class="text-sm italic text-muted">Ya lo reservaste</span>
                        <button type="button" (click)="cancelReservation(r)"
                          class="rounded border border-line px-2 py-0.5 text-xs font-medium text-out hover:bg-[#f6e7e3]">
                          Cancelar reserva
                        </button>
                      </div>
                    } @else {
                      <button type="button" (click)="reserve(book)"
                        class="rounded border border-amber bg-transparent px-3 py-1 text-sm font-medium text-[#8a6115] hover:bg-amber/10">
                        Reservar
                      </button>
                    }
                  }
                </div>
              }
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

  protected readonly searchForm = this.fb.nonNullable.group({ q: [''], status: [''] });
  protected readonly createForm = this.fb.nonNullable.group({
    title: ['', [Validators.required]],
    author: ['', [Validators.required]],
    isbn: ['', [Validators.required]],
    publishedYear: [null as number | null],
  });

  protected readonly editingId = signal<number | null>(null);
  protected readonly editForm = this.fb.nonNullable.group({
    title: ['', [Validators.required]],
    author: ['', [Validators.required]],
    publishedYear: [null as number | null],
  });

  // USUARIO: relación con cada libro. BIBLIOTECARIO: reservas por confirmar.
  protected readonly borrowedIds = signal<Set<number>>(new Set());
  protected readonly myReservations = signal<Reservation[]>([]);
  protected readonly pending = signal<Reservation[]>([]);

  // Préstamo directo a un tercero (BIBLIOTECARIO).
  protected readonly lendingId = signal<number | null>(null);
  protected readonly lendForm = this.fb.nonNullable.group({
    borrowerName: ['', [Validators.required]],
    borrowerEmail: ['', [Validators.required, Validators.email]],
  });

  ngOnInit(): void {
    this.load();
    this.loadRelations();
  }

  hasBorrowed(bookId: number): boolean {
    return this.borrowedIds().has(bookId);
  }
  hasReserved(bookId: number): boolean {
    return this.myReservations().some((r) => r.bookId === bookId);
  }
  myReservationFor(bookId: number): Reservation | undefined {
    return this.myReservations().find((r) => r.bookId === bookId);
  }
  pendingFor(bookId: number): Reservation | undefined {
    return this.pending().find((r) => r.bookId === bookId);
  }

  /** Carga la relación del usuario con los libros según su rol. */
  loadRelations(): void {
    if (this.auth.isUsuario()) {
      this.loansService.mine().subscribe({
        next: (loans) =>
          this.borrowedIds.set(new Set(loans.filter((l) => !l.returned).map((l) => l.bookId))),
        error: () => {},
      });
      this.reservationsService.mine().subscribe({
        next: (rs) => this.myReservations.set(rs),
        error: () => {},
      });
    } else if (this.auth.isBibliotecario()) {
      this.reservationsService.pending().subscribe({
        next: (rs) => this.pending.set(rs),
        error: () => {},
      });
    }
  }

  cancelReservation(reservation: Reservation): void {
    this.reservationsService.cancel(reservation.id).subscribe({
      next: () => {
        this.flash(`Cancelaste la reserva de: ${reservation.bookTitle}.`);
        this.load();
        this.loadRelations();
      },
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudo cancelar la reserva.')),
    });
  }

  startLend(book: Book): void {
    this.lendingId.set(book.id);
    this.lendForm.reset({ borrowerName: '', borrowerEmail: '' });
  }
  cancelLend(): void {
    this.lendingId.set(null);
  }
  submitLend(book: Book): void {
    if (this.lendForm.invalid) {
      this.lendForm.markAllAsTouched();
      return;
    }
    const { borrowerName, borrowerEmail } = this.lendForm.getRawValue();
    this.loansService.create({ bookId: book.id, borrowerName, borrowerEmail }).subscribe({
      next: () => {
        this.lendingId.set(null);
        this.flash(`Préstamo registrado a ${borrowerName}.`);
        this.load();
      },
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudo registrar el préstamo.')),
    });
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

  reserve(book: Book): void {
    this.reservationsService.create(book.id).subscribe({
      next: () => {
        this.flash(`Reservaste: ${book.title}. Retirá el libro dentro del plazo.`);
        this.load();
        this.loadRelations();
      },
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudo reservar.')),
    });
  }

  confirm(reservation: Reservation): void {
    this.loansService.confirm(reservation.id).subscribe({
      next: () => {
        this.flash(`Préstamo confirmado para ${reservation.requesterEmail}.`);
        this.load();
        this.loadRelations();
      },
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudo confirmar el préstamo.')),
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

  startEdit(book: Book): void {
    this.editingId.set(book.id);
    this.editForm.reset({ title: book.title, author: book.author, publishedYear: book.publishedYear });
  }
  cancelEdit(): void {
    this.editingId.set(null);
  }
  saveEdit(book: Book): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    this.booksService.update(book.id, this.editForm.getRawValue()).subscribe({
      next: () => {
        this.editingId.set(null);
        this.flash('Libro actualizado.');
        this.load();
      },
      error: (e) => this.error.set(apiErrorMessage(e, 'No se pudo actualizar el libro.')),
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
        return 'bg-[#e7efe7] text-ok';
      case 'PRESTADO':
        return 'bg-[#f6e7e3] text-out';
      default:
        return 'bg-[#f3e8cf] text-res';
    }
  }

  private flash(text: string | null): void {
    this.message.set(text);
    this.error.set(null);
  }
}
