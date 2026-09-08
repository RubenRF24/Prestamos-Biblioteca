package com.biblioteca.prestamosbiblioteca.book.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Respuesta de {@code GET /authors/{key}.json} (sólo necesitamos el nombre). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryAuthorResponse(String name) {
}
