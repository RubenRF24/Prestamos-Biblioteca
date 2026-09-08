package com.biblioteca.prestamosbiblioteca.loan.web;

import com.biblioteca.prestamosbiblioteca.loan.domain.Loan;
import java.time.Instant;

public record LoanDto(
        Long id,
        Long bookId,
        String bookTitle,
        String borrowerName,
        String borrowerEmail,
        Instant loanDate,
        Instant dueDate,
        Instant returnDate,
        boolean returned,
        boolean overdue) {

    public static LoanDto from(Loan loan) {
        return new LoanDto(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getBorrowerName(),
                loan.getBorrowerEmail(),
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.isReturned(),
                loan.isOverdue(Instant.now()));
    }
}
