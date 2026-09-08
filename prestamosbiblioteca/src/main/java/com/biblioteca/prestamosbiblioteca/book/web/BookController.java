package com.biblioteca.prestamosbiblioteca.book.web;

import com.biblioteca.prestamosbiblioteca.book.domain.BookService;
import com.biblioteca.prestamosbiblioteca.book.domain.BookStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /** Catálogo con búsqueda por título/autor y filtro por estado. */
    @GetMapping
    public List<BookDto> list(@RequestParam(required = false) String q,
                              @RequestParam(required = false) BookStatus status) {
        return bookService.search(q, status).stream().map(BookDto::from).toList();
    }

    @GetMapping("/{id}")
    public BookDto getById(@PathVariable Long id) {
        return BookDto.from(bookService.getEntity(id));
    }

    /** Previsualización desde Open Library para el botón "Autocompletar desde ISBN". */
    @GetMapping("/lookup/{isbn}")
    public ResponseEntity<BookPreviewDto> lookup(@PathVariable String isbn) {
        return bookService.previewByIsbn(isbn)
                .map(BookPreviewDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BookDto> create(@Valid @RequestBody CreateBookRequest request) {
        BookDto dto = BookDto.from(
                bookService.create(request.title(), request.author(), request.isbn(), request.publishedYear()));
        return ResponseEntity.created(URI.create("/api/books/" + dto.id())).body(dto);
    }

    @PutMapping("/{id}")
    public BookDto update(@PathVariable Long id, @Valid @RequestBody UpdateBookRequest request) {
        return BookDto.from(
                bookService.update(id, request.title(), request.author(), request.publishedYear()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
