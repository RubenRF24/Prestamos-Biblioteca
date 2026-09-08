package com.biblioteca.prestamosbiblioteca.user.web.dto;

import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;

public record AuthUserResponse(Long id, String name, String email, String role) {

    public static AuthUserResponse from(AppUser user) {
        return new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
