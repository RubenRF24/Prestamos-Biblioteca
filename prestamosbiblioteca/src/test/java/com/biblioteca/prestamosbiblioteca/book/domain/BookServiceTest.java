package com.biblioteca.prestamosbiblioteca.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.biblioteca.prestamosbiblioteca.book.infra.BookRepository;
import com.biblioteca.prestamosbiblioteca.book.infra.ExternalBookData;
import com.biblioteca.prestamosbiblioteca.book.infra.OpenLibraryClient;
import com.biblioteca.prestamosbiblioteca.shared.exception.BookNotAvailableException;
import com.biblioteca.prestamosbiblioteca.shared.exception.DuplicateIsbnException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private OpenLibraryClient openLibraryClient;
    @InjectMocks
    private BookService bookService;

    @Test
    void create_conIsbnDuplicado_lanzaExcepcion() {
        when(bookRepository.existsByIsbn("123")).thenReturn(true);

        assertThatThrownBy(() -> bookService.create("T", "A", "123", 2020))
                .isInstanceOf(DuplicateIsbnException.class);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void create_completaDatosFaltantesDesdeOpenLibrary() {
        when(bookRepository.existsByIsbn("123")).thenReturn(false);
        when(openLibraryClient.lookupByIsbn("123"))
                .thenReturn(Optional.of(new ExternalBookData("Externo", "AutorExt", 1999, "cover.jpg", "java, testing")));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        // El año viene null: debe completarse desde Open Library; título/autor manuales mandan.
        bookService.create("Titulo Manual", "Autor Manual", "123", null);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        Book saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Titulo Manual");
        assertThat(saved.getAuthor()).isEqualTo("Autor Manual");
        assertThat(saved.getPublishedYear()).isEqualTo(1999);
        assertThat(saved.getCoverUrl()).isEqualTo("cover.jpg");
        assertThat(saved.getSubjects()).isEqualTo("java, testing");
        assertThat(saved.getStatus()).isEqualTo(BookStatus.DISPONIBLE);
    }

    @Test
    void delete_libroNoDisponible_lanzaExcepcion() {
        Book prestado = Book.builder().id(1L).title("T").author("A").isbn("1").status(BookStatus.PRESTADO).build();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(prestado));

        assertThatThrownBy(() -> bookService.delete(1L))
                .isInstanceOf(BookNotAvailableException.class);
        verify(bookRepository, never()).delete(any());
    }
}
