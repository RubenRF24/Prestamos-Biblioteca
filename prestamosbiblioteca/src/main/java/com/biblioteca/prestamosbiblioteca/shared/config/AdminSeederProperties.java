package com.biblioteca.prestamosbiblioteca.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credenciales del ADMIN de arranque (configurables por env, con defaults de desarrollo). */
@ConfigurationProperties(prefix = "app.admin")
public record AdminSeederProperties(String name, String email, String password) {
}
