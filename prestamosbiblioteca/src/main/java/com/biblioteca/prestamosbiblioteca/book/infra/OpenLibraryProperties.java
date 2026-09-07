package com.biblioteca.prestamosbiblioteca.book.infra;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.openlibrary")
public record OpenLibraryProperties(String baseUrl, String coversUrl, Duration timeout) {
}
