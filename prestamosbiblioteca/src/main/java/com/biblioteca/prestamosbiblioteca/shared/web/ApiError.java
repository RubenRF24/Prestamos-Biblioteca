package com.biblioteca.prestamosbiblioteca.shared.web;

import java.time.Instant;

/** Cuerpo JSON uniforme para todos los errores de la API. */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path) {

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path);
    }
}
