package com.biblioteca.prestamosbiblioteca.reservation.web;

import com.biblioteca.prestamosbiblioteca.reservation.domain.Reservation;
import java.time.Instant;

public record ReservationDto(
        Long id,
        Long bookId,
        String bookTitle,
        String requesterEmail,
        String status,
        Instant requestedAt,
        Instant expiresAt) {

    public static ReservationDto from(Reservation reservation) {
        return new ReservationDto(
                reservation.getId(),
                reservation.getBook().getId(),
                reservation.getBook().getTitle(),
                reservation.getRequesterEmail(),
                reservation.getStatus().name(),
                reservation.getRequestedAt(),
                reservation.getExpiresAt());
    }
}
