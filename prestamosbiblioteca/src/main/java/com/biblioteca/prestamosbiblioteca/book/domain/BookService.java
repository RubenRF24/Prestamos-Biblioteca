package com.biblioteca.prestamosbiblioteca.book.domain;

import com.biblioteca.prestamosbiblioteca.book.infra.BookRepository;
import com.biblioteca.prestamosbiblioteca.book.infra.ExternalBookData;
import com.biblioteca.prestamosbiblioteca.book.infra.OpenLibraryClient;
import com.biblioteca.prestamosbiblioteca.shared.exception.BookNotAvailableException;
import com.biblioteca.prestamosbiblioteca.shared.exception.DuplicateIsbnException;
import com.biblioteca.prestamosbiblioteca.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final OpenLibraryClient openLibraryClient;

    public BookService(BookRepository bookRepository, OpenLibraryClient openLibraryClient) {
        this.bookRepository = bookRepository;
        this.openLibraryClient = openLibraryClient;
    }

    public List<Book> search(String query, BookStatus status) {
        String normalized = (query == null || query.isBlank()) ? "" : query.trim();
        return status == null
                ? bookRepository.searchByText(normalized)
                : bookRepository.searchByTextAndStatus(normalized, status);
    }

    public Book getEntity(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado: " + id));
    }

    /** Previsualización desde Open Library sin persistir nada. */
    public Optional<ExternalBookData> previewByIsbn(String isbn) {
        return openLibraryClient.lookupByIsbn(isbn);
    }

    /**
     * Alta de libro. El ISBN debe ser único. Los datos manuales mandan; el enriquecimiento
     * desde Open Library sólo completa portada, temas y año cuando faltan. Si la API falla,
     * el libro se guarda igual con lo que el usuario escribió.
     */
    @Transactional
    public Book create(String title, String author, String isbn, Integer publishedYear) {
        if (bookRepository.existsByIsbn(isbn)) {
            throw new DuplicateIsbnException(isbn);
        }
        Book book = Book.builder()
                .title(title)
                .author(author)
                .isbn(isbn)
                .publishedYear(publishedYear)
                .status(BookStatus.DISPONIBLE)
                .build();
        openLibraryClient.lookupByIsbn(isbn).ifPresent(external -> enrich(book, external));
        return bookRepository.save(book);
    }

    private void enrich(Book book, ExternalBookData external) {
        if (book.getPublishedYear() == null) {
            book.setPublishedYear(external.publishedYear());
        }
        if (book.getCoverUrl() == null) {
            book.setCoverUrl(external.coverUrl());
        }
        if (book.getSubjects() == null) {
            book.setSubjects(external.subjects());
        }
    }

    /** Baja de libro: sólo permitida si está DISPONIBLE. */
    @Transactional
    public void delete(Long id) {
        Book book = getEntity(id);
        if (book.getStatus() != BookStatus.DISPONIBLE) {
            throw new BookNotAvailableException("Sólo se pueden eliminar libros DISPONIBLES");
        }
        bookRepository.delete(book);
    }

    /** Cambio de estado usado por los dominios de préstamo y reserva. */
    @Transactional
    public Book changeStatus(Book book, BookStatus status) {
        book.setStatus(status);
        return bookRepository.save(book);
    }

    public long countByStatus(BookStatus status) {
        return bookRepository.countByStatus(status);
    }

    public long countAll() {
        return bookRepository.count();
    }
}
