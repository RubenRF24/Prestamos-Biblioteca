package com.biblioteca.prestamosbiblioteca.book.web;

import com.biblioteca.prestamosbiblioteca.book.domain.Book;
import java.util.Arrays;
import java.util.List;

public record BookDto(
        Long id,
        String title,
        String author,
        String isbn,
        Integer publishedYear,
        String status,
        String coverUrl,
        List<String> subjects) {

    public static BookDto from(Book book) {
        List<String> subjects = book.getSubjects() == null || book.getSubjects().isBlank()
                ? List.of()
                : Arrays.stream(book.getSubjects().split(",")).map(String::trim).toList();
        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublishedYear(),
                book.getStatus().name(),
                book.getCoverUrl(),
                subjects);
    }
}
