package com.biblioteca.prestamosbiblioteca.user.domain;

public enum Role {
    /** Gestiona catálogo, cuentas y estadísticas. No participa en préstamos. */
    ADMIN,
    /** Personal de mesa: confirma reservas en préstamos y ve quién tiene cada libro. */
    BIBLIOTECARIO,
    /** Se registra solo, reserva libros y recibe los préstamos. */
    USUARIO
}
