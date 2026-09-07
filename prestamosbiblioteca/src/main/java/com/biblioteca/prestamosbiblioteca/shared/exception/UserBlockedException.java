package com.biblioteca.prestamosbiblioteca.shared.exception;

import org.springframework.http.HttpStatus;

public class UserBlockedException extends BusinessException {

    public UserBlockedException(String message) {
        super("USER_BLOCKED", HttpStatus.FORBIDDEN, message);
    }
}
