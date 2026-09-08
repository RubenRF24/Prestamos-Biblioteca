package com.biblioteca.prestamosbiblioteca.book.infra;

/** Datos de un libro obtenidos desde la API externa (Open Library). */
public record ExternalBookData(
        String title,
        String author,
        Integer publishedYear,
        String coverUrl,
        String subjects) {
}
