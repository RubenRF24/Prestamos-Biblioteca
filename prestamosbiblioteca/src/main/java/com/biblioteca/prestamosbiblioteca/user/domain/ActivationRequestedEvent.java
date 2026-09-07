package com.biblioteca.prestamosbiblioteca.user.domain;

/** Se publica cuando se provisiona una cuenta que debe activarse por correo. */
public record ActivationRequestedEvent(Long userId) {
}
