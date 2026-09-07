package com.biblioteca.prestamosbiblioteca.book.web;

import com.biblioteca.prestamosbiblioteca.book.infra.ExternalBookData;
import java.util.Arrays;
import java.util.List;

/** Previsualización devuelta por el lookup de ISBN (sin persistir). */
public record BookPreviewDto(
        String title,
        String author,
        Integer publishedYear,
        String coverUrl,
        List<String> subjects) {

    public static BookPreviewDto from(ExternalBookData data) {
        List<String> subjects = data.subjects() == null || data.subjects().isBlank()
                ? List.of()
                : Arrays.stream(data.subjects().split(",")).map(String::trim).toList();
        return new BookPreviewDto(data.title(), data.author(), data.publishedYear(), data.coverUrl(), subjects);
    }
}
