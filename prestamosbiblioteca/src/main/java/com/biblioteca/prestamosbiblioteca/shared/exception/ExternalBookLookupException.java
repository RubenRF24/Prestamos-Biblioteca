package com.biblioteca.prestamosbiblioteca.shared.exception;

import org.springframework.http.HttpStatus;

public class ExternalBookLookupException extends BusinessException {

    public ExternalBookLookupException(String message) {
        super("EXTERNAL_BOOK_LOOKUP_FAILED", HttpStatus.BAD_GATEWAY, message);
    }
}
