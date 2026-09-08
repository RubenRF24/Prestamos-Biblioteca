package com.biblioteca.prestamosbiblioteca.user.web.dto;

import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;
import java.time.Instant;

public record BlockedUserResponse(Long id, String name, String email, Instant blockedUntil) {

    public static BlockedUserResponse from(AppUser user) {
        return new BlockedUserResponse(user.getId(), user.getName(), user.getEmail(), user.getBlockedUntil());
    }
}
