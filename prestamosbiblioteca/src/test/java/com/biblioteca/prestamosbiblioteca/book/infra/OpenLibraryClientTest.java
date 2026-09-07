package com.biblioteca.prestamosbiblioteca.book.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenLibraryClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private OpenLibraryClient clientPointingToServer() {
        String baseUrl = server.url("/").toString().replaceAll("/$", "");
        return new OpenLibraryClient(new OpenLibraryProperties(baseUrl, "https://covers", Duration.ofSeconds(3)));
    }

    @Test
    void lookup_respuestaOk_mapeaLosDatos() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"title":"Clean Code","publish_date":"2008",
                         "authors":[{"key":"/authors/OL1A"}],"subjects":["Software"]}
                        """));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"name\":\"Robert C. Martin\"}"));

        Optional<ExternalBookData> result = clientPointingToServer().lookupByIsbn("9780132350884");

        assertThat(result).isPresent();
        ExternalBookData data = result.get();
        assertThat(data.title()).isEqualTo("Clean Code");
        assertThat(data.author()).isEqualTo("Robert C. Martin");
        assertThat(data.publishedYear()).isEqualTo(2008);
        assertThat(data.coverUrl()).contains("9780132350884");
        assertThat(data.subjects()).contains("Software");
    }

    @Test
    void lookup_cuandoLaApiFalla_devuelveEmpty() {
        server.enqueue(new MockResponse().setResponseCode(500));

        Optional<ExternalBookData> result = clientPointingToServer().lookupByIsbn("0000000000");

        assertThat(result).isEmpty();
    }
}
