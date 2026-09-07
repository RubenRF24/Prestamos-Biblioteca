package com.biblioteca.prestamosbiblioteca.book.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Subconjunto de la respuesta de {@code GET /isbn/{isbn}.json} de Open Library. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenLibraryResponse(
        String title,
        @JsonProperty("publish_date") String publishDate,
        List<AuthorRef> authors,
        List<String> subjects) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorRef(String key) {
    }
}
