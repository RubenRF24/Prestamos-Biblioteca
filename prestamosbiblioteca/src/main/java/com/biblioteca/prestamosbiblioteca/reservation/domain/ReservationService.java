package com.biblioteca.prestamosbiblioteca.reservation.domain;

import com.biblioteca.prestamosbiblioteca.book.domain.Book;
import com.biblioteca.prestamosbiblioteca.book.domain.BookService;
import com.biblioteca.prestamosbiblioteca.book.domain.BookStatus;
import com.biblioteca.prestamosbiblioteca.reservation.infra.ReservationRepository;
import com.biblioteca.prestamosbiblioteca.shared.exception.BookNotAvailableException;
import com.biblioteca.prestamosbiblioteca.shared.exception.ResourceNotFoundException;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lista de espera con ventana de exclusividad para el primero de la fila. */
@Service
public class ReservationService {

    private static final List<ReservationStatus> ACTIVE_STATES =
            List.of(ReservationStatus.PENDIENTE, ReservationStatus.NOTIFICADO);

    private final ReservationRepository reservationRepository;
    private final BookService bookService;
    private final ReservationProperties properties;
    private final ApplicationEventPublisher events;

    public ReservationService(ReservationRepository reservationRepository,
                              BookService bookService,
                              ReservationProperties properties,
                              ApplicationEventPublisher events) {
        this.reservationRepository = reservationRepository;
        this.bookService = bookService;
        this.properties = properties;
        this.events = events;
    }

    /** Anotarse en la lista de espera de un libro que no está disponible. */
    @Transactional
    public Reservation create(Long bookId, AppUser requester) {
        Book book = bookService.getEntity(bookId);
        if (book.getStatus() == BookStatus.DISPONIBLE) {
            throw new BookNotAvailableException("El libro está disponible: pedilo directamente en préstamo");
        }
        if (reservationRepository.existsByBookIdAndRequesterIdAndStatusIn(bookId, requester.getId(), ACTIVE_STATES)) {
            throw new BookNotAvailableException("Ya tenés una reserva activa para este libro");
        }
        Reservation reservation = Reservation.builder()
                .book(book)
                .requester(requester)
                .requesterEmail(requester.getEmail())
                .requestedAt(Instant.now())
                .status(ReservationStatus.PENDIENTE)
                .build();
        return reservationRepository.save(reservation);
    }

    @Transactional
    public void cancel(Long reservationId, AppUser requester, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + reservationId));
        if (!isAdmin && !reservation.getRequester().getId().equals(requester.getId())) {
            throw new BookNotAvailableException("No podés cancelar una reserva que no es tuya");
        }
        boolean wasHolding = reservation.getStatus() == ReservationStatus.NOTIFICADO;
        reservation.setStatus(ReservationStatus.CANCELADO);
        reservationRepository.save(reservation);
        // Si retenía el libro, hay que pasarlo al siguiente o liberarlo.
        if (wasHolding) {
            releaseOrPromote(reservation.getBook());
        }
    }

    /** ¿Hay una reserva NOTIFICADO vigente para este libro a nombre de este usuario? */
    public boolean isHeldBy(Book book, AppUser borrower) {
        return reservationRepository.findFirstByBookIdAndStatus(book.getId(), ReservationStatus.NOTIFICADO)
                .filter(r -> r.getRequester().getId().equals(borrower.getId()))
                .filter(r -> r.getExpiresAt() == null || r.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    /** Marca como CUMPLIDA la reserva vigente cuando el titular finalmente presta el libro. */
    @Transactional
    public void fulfillHold(Book book, AppUser borrower) {
        reservationRepository.findFirstByBookIdAndStatus(book.getId(), ReservationStatus.NOTIFICADO)
                .filter(r -> r.getRequester().getId().equals(borrower.getId()))
                .ifPresent(r -> {
                    r.setStatus(ReservationStatus.CUMPLIDO);
                    reservationRepository.save(r);
                });
    }

    /**
     * Al devolverse un libro: si hay alguien esperando, notifica al primero y deja el libro
     * RESERVADO. Devuelve {@code true} si quedó reservado, {@code false} si nadie esperaba
     * (en ese caso el llamador lo marca DISPONIBLE).
     */
    @Transactional
    public boolean promoteNextForReturnedBook(Book book) {
        return reservationRepository
                .findFirstByBookIdAndStatusOrderByRequestedAtAsc(book.getId(), ReservationStatus.PENDIENTE)
                .map(next -> {
                    Instant now = Instant.now();
                    next.setStatus(ReservationStatus.NOTIFICADO);
                    next.setNotifiedAt(now);
                    next.setExpiresAt(now.plusSeconds(properties.holdHours() * 3600L));
                    reservationRepository.save(next);
                    bookService.changeStatus(book, BookStatus.RESERVADO);
                    events.publishEvent(new BookAvailableEvent(next.getId()));
                    return true;
                })
                .orElse(false);
    }

    /** Expira las reservas cuya ventana venció y promueve al siguiente (llamado por el scheduler). */
    @Transactional
    public int expireHolds() {
        List<Reservation> expired = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.NOTIFICADO, Instant.now());
        for (Reservation reservation : expired) {
            reservation.setStatus(ReservationStatus.CANCELADO);
            reservationRepository.save(reservation);
            releaseOrPromote(reservation.getBook());
        }
        return expired.size();
    }

    public Reservation getById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada: " + reservationId));
    }

    private void releaseOrPromote(Book book) {
        if (!promoteNextForReturnedBook(book)) {
            bookService.changeStatus(book, BookStatus.DISPONIBLE);
        }
    }
}
