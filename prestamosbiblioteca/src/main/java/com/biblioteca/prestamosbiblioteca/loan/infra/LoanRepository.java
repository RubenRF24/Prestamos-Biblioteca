package com.biblioteca.prestamosbiblioteca.loan.infra;

import com.biblioteca.prestamosbiblioteca.loan.domain.Loan;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByBorrowerIdOrderByLoanDateDesc(Long borrowerId);

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
