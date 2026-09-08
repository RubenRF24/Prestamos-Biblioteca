package com.biblioteca.prestamosbiblioteca.reservation.web;

import com.biblioteca.prestamosbiblioteca.reservation.domain.ReservationService;
import com.biblioteca.prestamosbiblioteca.shared.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    /** El lector reserva/solicita un libro. */
    @PostMapping
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<ReservationDto> create(@Valid @RequestBody CreateReservationRequest request,
                                                 @AuthenticationPrincipal AppUserPrincipal principal) {
        ReservationDto dto = ReservationDto.from(
                reservationService.create(request.bookId(), principal.getUser()));
        return ResponseEntity.created(URI.create("/api/reservations/" + dto.id())).body(dto);
    }

    /** Reservas activas del lector logueado. */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('USUARIO')")
    public List<ReservationDto> mine(@AuthenticationPrincipal AppUserPrincipal principal) {
        return reservationService.findMine(principal.getId()).stream().map(ReservationDto::from).toList();
    }

    /** Reservas retenidas a la espera de que el bibliotecario confirme el préstamo. */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public List<ReservationDto> pending() {
        return reservationService.findPendingConfirmation().stream().map(ReservationDto::from).toList();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USUARIO')")
    public ResponseEntity<Void> cancel(@PathVariable Long id,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        reservationService.cancel(id, principal.getUser(), false);
        return ResponseEntity.noContent().build();
    }
}
