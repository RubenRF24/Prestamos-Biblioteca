package com.biblioteca.prestamosbiblioteca.user.domain;

/** Se publica al crear una cuenta activa (auto-registro o alta por un ADMIN). */
public record UserRegisteredEvent(Long userId) {
}
