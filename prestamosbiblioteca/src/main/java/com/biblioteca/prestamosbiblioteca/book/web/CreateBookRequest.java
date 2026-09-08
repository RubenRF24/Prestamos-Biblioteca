package com.biblioteca.prestamosbiblioteca.book.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta de libro. El ISBN es obligatorio; título y autor son los datos manuales (siempre
 * presentes). El año es opcional: si no viene, se intenta completar desde Open Library.
 */
public record CreateBookRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 300) String author,
        @NotBlank @Size(max = 20) String isbn,
        Integer publishedYear) {
}
