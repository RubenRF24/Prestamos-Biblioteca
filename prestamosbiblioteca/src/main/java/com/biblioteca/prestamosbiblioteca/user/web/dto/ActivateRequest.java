package com.biblioteca.prestamosbiblioteca.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivateRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 100) String password) {
}
