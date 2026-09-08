package com.biblioteca.prestamosbiblioteca.shared.security;

import com.biblioteca.prestamosbiblioteca.shared.config.AppSecurityProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/** Construye la cookie httpOnly que transporta el JWT. */
@Service
public class AuthCookieService {

    private final AppSecurityProperties properties;

    public AuthCookieService(AppSecurityProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie build(String token) {
        return base(token, properties.jwt().expiration());
    }

    /** Cookie vacía con Max-Age 0 para cerrar sesión. */
    public ResponseCookie clear() {
        return base("", Duration.ZERO);
    }

    private ResponseCookie base(String value, Duration maxAge) {
        return ResponseCookie.from(properties.cookie().name(), value)
                .httpOnly(true)
                .secure(properties.cookie().secure())
                .sameSite(properties.cookie().sameSite())
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
