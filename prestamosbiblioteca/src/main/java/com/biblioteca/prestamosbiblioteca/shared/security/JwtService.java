package com.biblioteca.prestamosbiblioteca.shared.security;

import com.biblioteca.prestamosbiblioteca.shared.config.AppSecurityProperties;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Genera y valida los JWT firmados con HMAC-SHA256. */
@Service
public class JwtService {

    private final SecretKey key;
    private final AppSecurityProperties properties;

    public JwtService(AppSecurityProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AppUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.jwt().expiration());
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /** Devuelve el email (subject) si el token es válido, o {@code null} si no lo es. */
    public String extractEmailIfValid(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}
