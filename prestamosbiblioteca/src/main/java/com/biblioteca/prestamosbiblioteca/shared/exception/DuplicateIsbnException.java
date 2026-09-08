package com.biblioteca.prestamosbiblioteca.shared.exception;

import org.springframework.http.HttpStatus;

public class DuplicateIsbnException extends BusinessException {

    public DuplicateIsbnException(String isbn) {
        super("DUPLICATE_ISBN", HttpStatus.CONFLICT, "Ya existe un libro con el ISBN " + isbn);
    }
}
