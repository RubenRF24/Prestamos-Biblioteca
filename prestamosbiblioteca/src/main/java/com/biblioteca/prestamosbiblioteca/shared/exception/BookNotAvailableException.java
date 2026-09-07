package com.biblioteca.prestamosbiblioteca.shared.exception;

import org.springframework.http.HttpStatus;

public class BookNotAvailableException extends BusinessException {

    public BookNotAvailableException(String message) {
        super("BOOK_NOT_AVAILABLE", HttpStatus.CONFLICT, message);
    }
}
