package com.biblioteca.prestamosbiblioteca.reservation.domain;

/** Se publica cuando una reserva es notificada porque el libro quedó disponible para ella. */
public record BookAvailableEvent(Long reservationId) {
}
