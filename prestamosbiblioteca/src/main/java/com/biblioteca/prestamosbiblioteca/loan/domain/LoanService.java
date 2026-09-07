package com.biblioteca.prestamosbiblioteca.loan.domain;

import com.biblioteca.prestamosbiblioteca.book.domain.Book;
import com.biblioteca.prestamosbiblioteca.book.domain.BookService;
import com.biblioteca.prestamosbiblioteca.book.domain.BookStatus;
import com.biblioteca.prestamosbiblioteca.loan.infra.LoanRepository;
import com.biblioteca.prestamosbiblioteca.reservation.domain.ReservationService;
import com.biblioteca.prestamosbiblioteca.shared.exception.BookNotAvailableException;
import com.biblioteca.prestamosbiblioteca.shared.exception.LoanAlreadyReturnedException;
import com.biblioteca.prestamosbiblioteca.shared.exception.ResourceNotFoundException;
import com.biblioteca.prestamosbiblioteca.shared.exception.UserBlockedException;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUserService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookService bookService;
    private final AppUserService userService;
    private final ReservationService reservationService;
    private final LoanProperties properties;
    private final ApplicationEventPublisher events;

    public LoanService(LoanRepository loanRepository,
                       BookService bookService,
                       AppUserService userService,
                       ReservationService reservationService,
                       LoanProperties properties,
                       ApplicationEventPublisher events) {
        this.loanRepository = loanRepository;
        this.bookService = bookService;
        this.userService = userService;
        this.reservationService = reservationService;
        this.properties = properties;
        this.events = events;
    }

    /**
     * Registra un préstamo. El borrower es el usuario logueado (self-service) o, si se indica
     * un email de tercero, la cuenta correspondiente (provisionándola si no existe). Valida
     * disponibilidad del libro y que el borrower no esté bloqueado, calcula la fecha límite y
     * publica el evento de confirmación (el correo sale tras el commit, sin bloquear la petición).
     */
    @Transactional
    public Loan create(Long bookId, String borrowerName, String borrowerEmail, AppUser currentUser) {
        Book book = bookService.getEntity(bookId);
        AppUser borrower = resolveBorrower(borrowerName, borrowerEmail, currentUser);

        Instant now = Instant.now();
        userService.clearExpiredBlock(borrower);
        if (borrower.isCurrentlyBlocked(now)) {
            throw new UserBlockedException("La cuenta está bloqueada hasta " + borrower.getBlockedUntil());
        }

        boolean consumingHold = false;
        if (book.getStatus() == BookStatus.RESERVADO && reservationService.isHeldBy(book, borrower)) {
            consumingHold = true;
        } else if (book.getStatus() != BookStatus.DISPONIBLE) {
            throw new BookNotAvailableException("El libro no está disponible: " + book.getStatus());
        }

        Loan loan = Loan.builder()
                .book(book)
                .borrower(borrower)
                .borrowerName(borrower.getName())
                .borrowerEmail(borrower.getEmail())
                .loanDate(now)
                .dueDate(now.plus(Duration.ofDays(properties.loanDays())))
                .build();
        loan = loanRepository.save(loan);

        bookService.changeStatus(book, BookStatus.PRESTADO);
        if (consumingHold) {
            reservationService.fulfillHold(book, borrower);
        }

        events.publishEvent(new LoanCreatedEvent(loan.getId()));
        return loan;
    }

    /** Devolución. Contabiliza atrasos, bloquea si corresponde y gestiona la lista de espera. */
    @Transactional
    public Loan returnLoan(Long loanId) {
        Loan loan = getEntity(loanId);
        if (loan.isReturned()) {
            throw new LoanAlreadyReturnedException();
        }
        Instant now = Instant.now();
        loan.setReturnDate(now);
        loanRepository.save(loan);

        if (loan.isReturnedLate()) {
            applyOverduePolicy(loan.getBorrower(), now);
        }

        Book book = loan.getBook();
        if (!reservationService.promoteNextForReturnedBook(book)) {
            bookService.changeStatus(book, BookStatus.DISPONIBLE);
        }
        return loan;
    }

    private void applyOverduePolicy(AppUser borrower, Instant now) {
        Instant since = now.minus(Duration.ofDays(properties.overdueWindowDays()));
        long lateReturns = loanRepository.countLateReturnsSince(borrower.getId(), since);
        if (lateReturns >= properties.maxOverdues() && !borrower.isCurrentlyBlocked(now)) {
            userService.applyBlock(borrower, now.plus(Duration.ofDays(properties.blockDays())));
        }
    }

    private AppUser resolveBorrower(String borrowerName, String borrowerEmail, AppUser currentUser) {
        if (borrowerEmail == null || borrowerEmail.isBlank()
                || borrowerEmail.equalsIgnoreCase(currentUser.getEmail())) {
            return currentUser;
        }
        String name = (borrowerName == null || borrowerName.isBlank()) ? borrowerEmail : borrowerName;
        return userService.findOrProvision(name, borrowerEmail);
    }

    public Loan getEntity(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado: " + loanId));
    }

    public List<Loan> findByBorrower(Long borrowerId) {
        return loanRepository.findByBorrowerIdOrderByLoanDateDesc(borrowerId);
    }

    public long countOverdue(Instant now) {
        return loanRepository.countByReturnDateIsNullAndDueDateBefore(now);
    }

    /** Préstamos por vencer entre {@code from} y {@code to} sin recordatorio enviado. */
    public List<Loan> findDueSoonNeedingReminder(Instant from, Instant to) {
        return loanRepository.findDueSoonWithoutReminder(from, to);
    }

    @Transactional
    public void markReminderSent(Long loanId) {
        Loan loan = getEntity(loanId);
        loan.setReminderSentAt(Instant.now());
        loanRepository.save(loan);
    }
}
