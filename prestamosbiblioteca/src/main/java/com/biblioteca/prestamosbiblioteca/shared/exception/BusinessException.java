package com.biblioteca.prestamosbiblioteca.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Base de las excepciones de negocio. Cada una expone un código estable y el
 * HTTP status con el que el {@code GlobalExceptionHandler} la traduce.
 */
public abstract class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    protected BusinessException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
