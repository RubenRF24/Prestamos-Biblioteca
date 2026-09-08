package com.biblioteca.prestamosbiblioteca.user.domain;

/** Se publica cuando una cuenta queda bloqueada por acumular atrasos. */
public record UserBlockedEvent(Long userId) {
}
