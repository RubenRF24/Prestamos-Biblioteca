package com.biblioteca.prestamosbiblioteca.reservation.infra;

import com.biblioteca.prestamosbiblioteca.reservation.domain.Reservation;
import com.biblioteca.prestamosbiblioteca.reservation.domain.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // JOIN FETCH del libro: el DTO se arma fuera de sesión (open-in-view=false).
    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.book
            WHERE r.requester.id = :requesterId AND r.status IN :statuses
            ORDER BY r.requestedAt DESC
            """)
    List<Reservation> findByRequesterIdAndStatusIn(@Param("requesterId") Long requesterId,
                                                   @Param("statuses") List<ReservationStatus> statuses);

    // JOIN FETCH del libro para armar el DTO fuera de sesión.
    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.book
            WHERE r.status = :status
            ORDER BY r.notifiedAt ASC
            """)
    List<Reservation> findByStatusOrderByNotifiedAtAsc(@Param("status") ReservationStatus status);

    /** Primero de la fila (FIFO) para un libro en un estado dado. */
    Optional<Reservation> findFirstByBookIdAndStatusOrderByRequestedAtAsc(Long bookId, ReservationStatus status);

    /** Reserva vigente (NOTIFICADO) que retiene un libro. */
    Optional<Reservation> findFirstByBookIdAndStatus(Long bookId, ReservationStatus status);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);

    boolean existsByBookIdAndRequesterIdAndStatusIn(Long bookId, Long requesterId, List<ReservationStatus> statuses);
}
