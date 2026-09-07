package com.biblioteca.prestamosbiblioteca.loan.web;

import com.biblioteca.prestamosbiblioteca.loan.domain.LoanService;
import com.biblioteca.prestamosbiblioteca.shared.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanDto> create(@Valid @RequestBody CreateLoanRequest request,
                                          @AuthenticationPrincipal AppUserPrincipal principal) {
        LoanDto dto = LoanDto.from(
                loanService.create(request.bookId(), request.borrowerName(), request.borrowerEmail(),
                        principal.getUser()));
        return ResponseEntity.created(URI.create("/api/loans/" + dto.id())).body(dto);
    }

    @GetMapping("/mine")
    public List<LoanDto> mine(@AuthenticationPrincipal AppUserPrincipal principal) {
        return loanService.findByBorrower(principal.getId()).stream().map(LoanDto::from).toList();
    }

    @PutMapping("/{id}/return")
    public LoanDto returnLoan(@PathVariable Long id) {
        return LoanDto.from(loanService.returnLoan(id));
    }
}
