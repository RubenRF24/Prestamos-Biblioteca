package com.biblioteca.prestamosbiblioteca.loan.domain;

/** Se publica al crear un préstamo; dispara el correo de confirmación (asíncrono). */
public record LoanCreatedEvent(Long loanId) {
}
