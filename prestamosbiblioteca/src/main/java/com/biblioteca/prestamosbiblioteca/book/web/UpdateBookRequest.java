package com.biblioteca.prestamosbiblioteca.book.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBookRequest(
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 300) String author,
        Integer publishedYear) {
}
