package com.biblioteca.prestamosbiblioteca.user.web;

import com.biblioteca.prestamosbiblioteca.user.domain.AppUserService;
import com.biblioteca.prestamosbiblioteca.user.web.dto.AuthUserResponse;
import com.biblioteca.prestamosbiblioteca.user.web.dto.BlockedUserResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de cuentas por parte del ADMIN. Protegido por reglas en SecurityConfig. */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AppUserService userService;

    public AdminUserController(AppUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/blocked")
    public List<BlockedUserResponse> blocked() {
        return userService.findBlocked().stream()
                .map(BlockedUserResponse::from)
                .toList();
    }

    @PutMapping("/{id}/unblock")
    public AuthUserResponse unblock(@PathVariable Long id) {
        return AuthUserResponse.from(userService.unblock(id));
    }
}
