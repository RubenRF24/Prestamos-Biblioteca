package com.biblioteca.prestamosbiblioteca.reservation.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parámetros de la lista de espera ({@code app.reservations.*}). */
@ConfigurationProperties(prefix = "app.reservations")
public record ReservationProperties(int holdHours) {
}
