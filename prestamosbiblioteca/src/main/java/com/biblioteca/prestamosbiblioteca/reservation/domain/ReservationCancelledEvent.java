package com.biblioteca.prestamosbiblioteca.reservation.domain;

/** Se publica cuando una reserva se cancela. */
public record ReservationCancelledEvent(Long reservationId) {
}
