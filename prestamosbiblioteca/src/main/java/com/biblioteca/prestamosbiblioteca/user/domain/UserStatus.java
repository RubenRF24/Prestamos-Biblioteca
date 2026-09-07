package com.biblioteca.prestamosbiblioteca.user.domain;

public enum UserStatus {
    /** Cuenta operativa: puede iniciar sesión. */
    ACTIVE,
    /** Cuenta provisional creada al prestar a un tercero: aún no puede iniciar sesión. */
    PENDING_ACTIVATION
}
