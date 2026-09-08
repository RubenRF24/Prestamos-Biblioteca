package com.biblioteca.prestamosbiblioteca.loan.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parámetros de negocio de los préstamos ({@code app.loans.*}). */
@ConfigurationProperties(prefix = "app.loans")
public record LoanProperties(
        int loanDays,
        int maxOverdues,
        int overdueWindowDays,
        int blockDays) {
}
