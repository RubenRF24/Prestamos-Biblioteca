package com.biblioteca.prestamosbiblioteca.loan.infra;

import com.biblioteca.prestamosbiblioteca.loan.domain.Loan;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Préstamos activos (no devueltos) paginados, con el libro cargado, para la vista del bibliotecario.
    @Query(value = "SELECT l FROM Loan l JOIN FETCH l.book WHERE l.returnDate IS NULL",
            countQuery = "SELECT COUNT(l) FROM Loan l WHERE l.returnDate IS NULL")
    Page<Loan> findActiveWithBook(Pageable pageable);

    // JOIN FETCH del libro: el DTO se arma fuera de la sesión (open-in-view=false),
    // así evitamos LazyInitializationException al leer loan.getBook().
    @Query("""
            SELECT l FROM Loan l
            JOIN FETCH l.book
            WHERE l.borrower.id = :borrowerId
            ORDER BY l.loanDate DESC
            """)
    List<Loan> findByBorrowerIdOrderByLoanDateDesc(@Param("borrowerId") Long borrowerId);

    boolean existsByBookIdAndBorrowerIdAndReturnDateIsNull(Long bookId, Long borrowerId);

    /** Cantidad de devoluciones tardías de un usuario dentro de una ventana temporal. */
    @Query("""
            SELECT COUNT(l) FROM Loan l
            WHERE l.borrower.id = :borrowerId
              AND l.returnDate IS NOT NULL
              AND l.returnDate > l.dueDate
              AND l.returnDate >= :since
            """)
    long countLateReturnsSince(@Param("borrowerId") Long borrowerId, @Param("since") Instant since);

    /** Préstamos por vencer (aún no devueltos) sin recordatorio enviado. */
    @Query("""
            SELECT l FROM Loan l
            WHERE l.returnDate IS NULL
              AND l.reminderSentAt IS NULL
              AND l.dueDate BETWEEN :from AND :to
            """)
    List<Loan> findDueSoonWithoutReminder(@Param("from") Instant from, @Param("to") Instant to);

    long countByReturnDateIsNullAndDueDateBefore(Instant now);
}
