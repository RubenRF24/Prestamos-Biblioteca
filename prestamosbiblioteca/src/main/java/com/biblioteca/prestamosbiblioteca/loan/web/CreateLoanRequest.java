package com.biblioteca.prestamosbiblioteca.loan.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Préstamo directo a un tercero, hecho por el bibliotecario en la mesa. Se indica el libro y a quién
 * se le presta (nombre + email). Si la persona no tiene cuenta, se crea como USUARIO (provisional).
 */
public record CreateLoanRequest(
        @NotNull Long bookId,
        @NotBlank @Size(max = 150) String borrowerName,
        @NotBlank @Email @Size(max = 255) String borrowerEmail) {
}
