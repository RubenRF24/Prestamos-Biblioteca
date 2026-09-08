package com.biblioteca.prestamosbiblioteca.loan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.biblioteca.prestamosbiblioteca.book.domain.Book;
import com.biblioteca.prestamosbiblioteca.book.domain.BookService;
import com.biblioteca.prestamosbiblioteca.book.domain.BookStatus;
import com.biblioteca.prestamosbiblioteca.loan.infra.LoanRepository;
import com.biblioteca.prestamosbiblioteca.reservation.domain.ReservationService;
import com.biblioteca.prestamosbiblioteca.shared.exception.BookNotAvailableException;
import com.biblioteca.prestamosbiblioteca.shared.exception.UserBlockedException;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUserService;
import com.biblioteca.prestamosbiblioteca.user.domain.Role;
import com.biblioteca.prestamosbiblioteca.user.domain.UserStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private BookService bookService;
    @Mock
    private AppUserService userService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private org.springframework.context.ApplicationEventPublisher events;

    private final LoanProperties properties = new LoanProperties(14, 3, 90, 7);

    private LoanService loanService() {
        return new LoanService(loanRepository, bookService, userService, reservationService, properties, events);
    }

    private AppUser activeUser() {
        return AppUser.builder().id(1L).name("Ana").email("ana@mail.com")
                .role(Role.BIBLIOTECARIO).status(UserStatus.ACTIVE).blocked(false).build();
    }

    private Book availableBook() {
        return Book.builder().id(10L).title("Clean Code").author("Martin").isbn("1")
                .status(BookStatus.DISPONIBLE).build();
    }

    @Test
    void create_selfService_marcaPrestadoYFijaVencimientoA14Dias() {
        AppUser user = activeUser();
        Book book = availableBook();
        when(bookService.getEntity(10L)).thenReturn(book);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setId(100L);
            return l;
        });

        Loan loan = loanService().create(10L, null, null, user);

        assertThat(Duration.between(loan.getLoanDate(), loan.getDueDate()).toDays()).isEqualTo(14);
        assertThat(loan.getBorrower()).isEqualTo(user);
        verify(bookService).changeStatus(book, BookStatus.PRESTADO);
        verify(events).publishEvent(any(LoanCreatedEvent.class));
    }

    @Test
    void create_usuarioBloqueado_lanzaExcepcion() {
        AppUser blocked = activeUser();
        blocked.setBlocked(true);
        blocked.setBlockedUntil(Instant.now().plus(Duration.ofDays(3)));
        when(bookService.getEntity(10L)).thenReturn(availableBook());

        assertThatThrownBy(() -> loanService().create(10L, null, null, blocked))
                .isInstanceOf(UserBlockedException.class);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void create_libroNoDisponible_lanzaExcepcion() {
        Book prestado = availableBook();
        prestado.setStatus(BookStatus.PRESTADO);
        when(bookService.getEntity(10L)).thenReturn(prestado);

        assertThatThrownBy(() -> loanService().create(10L, null, null, activeUser()))
                .isInstanceOf(BookNotAvailableException.class);
    }

    @Test
    void returnLoan_tercerAtrasoEn90Dias_bloqueaLaCuenta() {
        AppUser user = activeUser();
        Book book = availableBook();
        book.setStatus(BookStatus.PRESTADO);
        Instant now = Instant.now();
        Loan loan = Loan.builder().id(100L).book(book).borrower(user)
                .borrowerName("Ana").borrowerEmail("ana@mail.com")
                .loanDate(now.minus(Duration.ofDays(20)))
                .dueDate(now.minus(Duration.ofDays(6))) // vencido -> devolución tardía
                .build();
        when(loanRepository.findById(100L)).thenReturn(java.util.Optional.of(loan));
        when(loanRepository.countLateReturnsSince(eq(1L), any())).thenReturn(3L);
        when(reservationService.promoteNextForReturnedBook(book)).thenReturn(false);

        loanService().returnLoan(100L);

        ArgumentCaptor<Instant> until = ArgumentCaptor.forClass(Instant.class);
        verify(userService).applyBlock(eq(user), until.capture());
        // El bloqueo es por 7 días hacia adelante.
        assertThat(until.getValue()).isAfter(Instant.now().plus(Duration.ofDays(6)));
        verify(bookService).changeStatus(book, BookStatus.DISPONIBLE);
    }
}
