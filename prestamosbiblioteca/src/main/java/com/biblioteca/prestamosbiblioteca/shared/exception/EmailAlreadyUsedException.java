package com.biblioteca.prestamosbiblioteca.shared.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyUsedException extends BusinessException {

    public EmailAlreadyUsedException(String email) {
        super("EMAIL_ALREADY_USED", HttpStatus.CONFLICT, "El correo ya está registrado: " + email);
    }
}
