package com.biblioteca.prestamosbiblioteca.loan.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de préstamo. Sin {@code borrowerEmail} el préstamo es para el usuario logueado
 * (self-service). Con un email de tercero, se presta a esa persona (creando su cuenta
 * provisional si no existe).
 */
public record CreateLoanRequest(
        @NotNull Long bookId,
        @Size(max = 150) String borrowerName,
        @Email @Size(max = 255) String borrowerEmail) {
}
