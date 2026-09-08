package com.biblioteca.prestamosbiblioteca.reservation.domain;

/** Se publica cuando un lector registra una reserva. */
public record ReservationCreatedEvent(Long reservationId) {
}
