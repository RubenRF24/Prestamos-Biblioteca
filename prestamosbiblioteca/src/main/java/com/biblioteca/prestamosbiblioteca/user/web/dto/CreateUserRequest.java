package com.biblioteca.prestamosbiblioteca.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta de cuenta por parte de un ADMIN. El rol es opcional (por defecto BIBLIOTECARIO). */
public record CreateUserRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        String role) {
}
