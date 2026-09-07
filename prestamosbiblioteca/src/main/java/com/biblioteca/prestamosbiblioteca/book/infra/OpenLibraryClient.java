package com.biblioteca.prestamosbiblioteca.book.infra;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente de Open Library. El resultado de un ISBN casi no cambia, así que se cachea
 * (Caffeine). Ante timeout o fallo devuelve {@link Optional#empty()} para que el alta del
 * libro no se rompa: se guarda con los datos manuales (documentado en el README).
 */
@Component
public class OpenLibraryClient {

    private static final Logger log = LoggerFactory.getLogger(OpenLibraryClient.class);
    private static final Pattern YEAR = Pattern.compile("(\\d{4})");
    private static final int MAX_SUBJECTS = 5;

    private final RestClient restClient;
    private final String coversUrl;

    public OpenLibraryClient(OpenLibraryProperties properties) {
        int timeoutMs = (int) properties.timeout().toMillis();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
        this.coversUrl = properties.coversUrl();
    }

    @Cacheable(cacheNames = "openLibraryLookup", key = "#isbn", unless = "#result == null || !#result.isPresent()")
    public Optional<ExternalBookData> lookupByIsbn(String isbn) {
        try {
            OpenLibraryResponse response = restClient.get()
                    .uri("/isbn/{isbn}.json", isbn)
                    .retrieve()
                    .body(OpenLibraryResponse.class);
            if (response == null || response.title() == null) {
                return Optional.empty();
            }
            ExternalBookData data = new ExternalBookData(
                    response.title(),
                    resolveAuthor(response.authors()),
                    parseYear(response.publishDate()),
                    coversUrl + "/b/isbn/" + isbn + "-L.jpg",
                    joinSubjects(response.subjects()));
            return Optional.of(data);
        } catch (Exception ex) {
            log.warn("Fallo consultando Open Library para ISBN {}: {}", isbn, ex.toString());
            return Optional.empty();
        }
    }

    /** Resuelve el nombre del primer autor con una segunda llamada; si falla, queda null. */
    private String resolveAuthor(List<OpenLibraryResponse.AuthorRef> authors) {
        if (authors == null || authors.isEmpty() || authors.get(0).key() == null) {
            return null;
        }
        try {
            OpenLibraryAuthorResponse author = restClient.get()
                    .uri(authors.get(0).key() + ".json")
                    .retrieve()
                    .body(OpenLibraryAuthorResponse.class);
            return author != null ? author.name() : null;
        } catch (Exception ex) {
            log.debug("No se pudo resolver el autor: {}", ex.toString());
            return null;
        }
    }

    private Integer parseYear(String publishDate) {
        if (publishDate == null) {
            return null;
        }
        Matcher matcher = YEAR.matcher(publishDate);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private String joinSubjects(List<String> subjects) {
        if (subjects == null || subjects.isEmpty()) {
            return null;
        }
        return String.join(", ", subjects.stream().limit(MAX_SUBJECTS).toList());
    }
}
