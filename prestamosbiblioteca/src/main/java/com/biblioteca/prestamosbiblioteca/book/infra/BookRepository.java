package com.biblioteca.prestamosbiblioteca.book.infra;

import com.biblioteca.prestamosbiblioteca.book.domain.Book;
import com.biblioteca.prestamosbiblioteca.book.domain.BookStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByIsbn(String isbn);

    Optional<Book> findByIsbn(String isbn);

    long countByStatus(BookStatus status);

    // Nota: se evita comparar parámetros contra NULL en el WHERE porque Postgres no puede
    // inferir el tipo de un bind nulo (falla con "lower(bytea)"). Se normaliza q a "" (que con
    // LIKE '%%' matchea todo) y se ramifica por status en el service.

    @Query("""
            SELECT b FROM Book b
            WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY b.title ASC
            """)
    List<Book> searchByText(@Param("q") String q);

    @Query("""
            SELECT b FROM Book b
            WHERE (LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(b.author) LIKE LOWER(CONCAT('%', :q, '%')))
              AND b.status = :status
            ORDER BY b.title ASC
            """)
    List<Book> searchByTextAndStatus(@Param("q") String q, @Param("status") BookStatus status);
}
