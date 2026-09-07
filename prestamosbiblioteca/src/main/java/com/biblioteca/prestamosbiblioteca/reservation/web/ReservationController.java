package com.biblioteca.prestamosbiblioteca.reservation.web;

import com.biblioteca.prestamosbiblioteca.reservation.domain.ReservationService;
import com.biblioteca.prestamosbiblioteca.shared.security.AppUserPrincipal;
import com.biblioteca.prestamosbiblioteca.user.domain.Role;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationDto> create(@Valid @RequestBody CreateReservationRequest request,
                                                 @AuthenticationPrincipal AppUserPrincipal principal) {
        ReservationDto dto = ReservationDto.from(
                reservationService.create(request.bookId(), principal.getUser()));
        return ResponseEntity.created(URI.create("/api/reservations/" + dto.id())).body(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        boolean isAdmin = principal.getUser().getRole() == Role.ADMIN;
        reservationService.cancel(id, principal.getUser(), isAdmin);
        return ResponseEntity.noContent().build();
    }
}
