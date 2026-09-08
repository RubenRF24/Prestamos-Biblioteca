package com.biblioteca.prestamosbiblioteca.loan.web;

import com.biblioteca.prestamosbiblioteca.loan.domain.LoanService;
import com.biblioteca.prestamosbiblioteca.shared.security.AppUserPrincipal;
import com.biblioteca.prestamosbiblioteca.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    /** Préstamos del lector logueado. */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('USUARIO')")
    public List<LoanDto> mine(@AuthenticationPrincipal AppUserPrincipal principal) {
        return loanService.findByBorrower(principal.getId()).stream().map(LoanDto::from).toList();
    }

    /** Préstamo directo a un tercero por el bibliotecario (crea la cuenta USUARIO si no existe). */
    @PostMapping
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<LoanDto> create(@Valid @RequestBody CreateLoanRequest request,
                                          @AuthenticationPrincipal AppUserPrincipal principal) {
        LoanDto dto = LoanDto.from(loanService.create(
                request.bookId(), request.borrowerName(), request.borrowerEmail(), principal.getUser()));
        return ResponseEntity.created(URI.create("/api/loans/" + dto.id())).body(dto);
    }

    /** Lista paginada de préstamos activos (quién tiene cada libro), para el bibliotecario. */
    @GetMapping("/active")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public PageResponse<LoanDto> active(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return PageResponse.of(loanService.findActive(PageRequest.of(page, Math.min(size, 100))), LoanDto::from);
    }

    /** El bibliotecario confirma una reserva retenida y arranca el préstamo. */
    @PostMapping("/confirm/{reservationId}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public LoanDto confirm(@PathVariable Long reservationId) {
        return LoanDto.from(loanService.confirmReservation(reservationId));
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public LoanDto returnLoan(@PathVariable Long id) {
        return LoanDto.from(loanService.returnLoan(id));
    }
}
