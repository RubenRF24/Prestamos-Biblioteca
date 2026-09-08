package com.biblioteca.prestamosbiblioteca.notification.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Remitente de los correos ({@code app.mail.*}). */
@ConfigurationProperties(prefix = "app.mail")
public record AppMailProperties(String from) {
}
