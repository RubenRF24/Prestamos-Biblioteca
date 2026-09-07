package com.biblioteca.prestamosbiblioteca.reservation.web;

import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(@NotNull Long bookId) {
}
