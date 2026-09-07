package com.biblioteca.prestamosbiblioteca.reservation.infra;

import com.biblioteca.prestamosbiblioteca.reservation.domain.Reservation;
import com.biblioteca.prestamosbiblioteca.reservation.domain.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Primero de la fila (FIFO) para un libro en un estado dado. */
    Optional<Reservation> findFirstByBookIdAndStatusOrderByRequestedAtAsc(Long bookId, ReservationStatus status);

    /** Reserva vigente (NOTIFICADO) que retiene un libro. */
    Optional<Reservation> findFirstByBookIdAndStatus(Long bookId, ReservationStatus status);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);

    boolean existsByBookIdAndRequesterIdAndStatusIn(Long bookId, Long requesterId, List<ReservationStatus> statuses);
}
