package com.biblioteca.prestamosbiblioteca.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binding de las propiedades {@code app.security.*}. */
@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(Jwt jwt, Cookie cookie) {

    public record Jwt(String secret, Duration expiration) {
    }

    public record Cookie(String name, boolean secure, String sameSite) {
    }
}
