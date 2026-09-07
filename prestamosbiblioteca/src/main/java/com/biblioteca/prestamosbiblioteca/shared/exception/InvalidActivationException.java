package com.biblioteca.prestamosbiblioteca.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidActivationException extends BusinessException {

    public InvalidActivationException(String message) {
        super("INVALID_ACTIVATION_TOKEN", HttpStatus.BAD_REQUEST, message);
    }
}
