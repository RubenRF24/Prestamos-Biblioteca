package com.biblioteca.prestamosbiblioteca.notification.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** URL base del frontend, usada para los links de los correos ({@code app.frontend.*}). */
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(String baseUrl) {
}
