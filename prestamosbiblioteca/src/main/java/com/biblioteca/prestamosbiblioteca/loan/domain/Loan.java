package com.biblioteca.prestamosbiblioteca.loan.domain;

import com.biblioteca.prestamosbiblioteca.book.domain.Book;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /** Titular del préstamo. Siempre es un AppUser (self-service o cuenta provisional). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private AppUser borrower;

    /** Snapshot del nombre al momento del préstamo (historial estable). */
    @Column(name = "borrower_name", nullable = false, length = 150)
    private String borrowerName;

    /** Snapshot del email al momento del préstamo. */
    @Column(name = "borrower_email", nullable = false)
    private String borrowerEmail;

    @Column(name = "loan_date", nullable = false)
    private Instant loanDate;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(name = "return_date")
    private Instant returnDate;

    /** Evita repetir el recordatorio de vencimiento. */
    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    /** Evita repetir el aviso de vencido. */
    @Column(name = "overdue_notice_sent_at")
    private Instant overdueNoticeSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public boolean isReturned() {
        return returnDate != null;
    }

    public boolean isReturnedLate() {
        return returnDate != null && returnDate.isAfter(dueDate);
    }

    public boolean isOverdue(Instant now) {
        return returnDate == null && dueDate.isBefore(now);
    }
}
