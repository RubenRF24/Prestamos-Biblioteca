package com.biblioteca.prestamosbiblioteca.admin.web;

public record StatsResponse(
        long totalBooks,
        long available,
        long borrowed,
        long reserved,
        long overdueLoans,
        long blockedUsers) {
}
