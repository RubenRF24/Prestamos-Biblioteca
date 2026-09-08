package com.biblioteca.prestamosbiblioteca.shared.exception;

import org.springframework.http.HttpStatus;

public class LoanAlreadyReturnedException extends BusinessException {

    public LoanAlreadyReturnedException() {
        super("LOAN_ALREADY_RETURNED", HttpStatus.CONFLICT, "El préstamo ya fue devuelto");
    }
}
