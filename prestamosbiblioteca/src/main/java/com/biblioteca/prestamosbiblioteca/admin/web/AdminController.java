package com.biblioteca.prestamosbiblioteca.admin.web;

import com.biblioteca.prestamosbiblioteca.book.domain.BookService;
import com.biblioteca.prestamosbiblioteca.book.domain.BookStatus;
import com.biblioteca.prestamosbiblioteca.loan.domain.LoanService;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUserService;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Estadísticas globales para el ADMIN. Protegido por reglas en SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final BookService bookService;
    private final LoanService loanService;
    private final AppUserService userService;

    public AdminController(BookService bookService, LoanService loanService, AppUserService userService) {
        this.bookService = bookService;
        this.loanService = loanService;
        this.userService = userService;
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return new StatsResponse(
                bookService.countAll(),
                bookService.countByStatus(BookStatus.DISPONIBLE),
                bookService.countByStatus(BookStatus.PRESTADO),
                bookService.countByStatus(BookStatus.RESERVADO),
                loanService.countOverdue(Instant.now()),
                userService.findBlocked().size());
    }
}
