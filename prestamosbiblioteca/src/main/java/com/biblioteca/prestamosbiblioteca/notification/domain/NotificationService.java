package com.biblioteca.prestamosbiblioteca.notification.domain;

import com.biblioteca.prestamosbiblioteca.loan.domain.Loan;
import com.biblioteca.prestamosbiblioteca.loan.domain.LoanService;
import com.biblioteca.prestamosbiblioteca.notification.infra.ThymeleafMailSender;
import com.biblioteca.prestamosbiblioteca.reservation.domain.Reservation;
import com.biblioteca.prestamosbiblioteca.reservation.domain.ReservationService;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUserService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquesta los cinco correos del sistema renderizando plantillas Thymeleaf.
 * Métodos transaccionales de solo lectura: corren en el hilo async con la sesión abierta,
 * para poder acceder a las asociaciones LAZY (p. ej. {@code loan.getBook()}) al armar el correo.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

    private final ThymeleafMailSender mailSender;
    private final LoanService loanService;
    private final AppUserService userService;
    private final ReservationService reservationService;
    private final FrontendProperties frontendProperties;

    public NotificationService(ThymeleafMailSender mailSender,
                               LoanService loanService,
                               AppUserService userService,
                               ReservationService reservationService,
                               FrontendProperties frontendProperties) {
        this.mailSender = mailSender;
        this.loanService = loanService;
        this.userService = userService;
        this.reservationService = reservationService;
        this.frontendProperties = frontendProperties;
    }

    public void sendLoanConfirmation(Long loanId) {
        Loan loan = loanService.getEntity(loanId);
        Map<String, Object> vars = new HashMap<>();
        vars.put("borrowerName", loan.getBorrowerName());
        vars.put("bookTitle", loan.getBook().getTitle());
        vars.put("loanDate", DATE.format(loan.getLoanDate()));
        vars.put("dueDate", DATE.format(loan.getDueDate()));
        mailSender.send(loan.getBorrowerEmail(),
                "Confirmación de préstamo: " + loan.getBook().getTitle(),
                "email/loan-confirmation", vars);
    }

    public void sendDueSoonReminder(Long loanId) {
        Loan loan = loanService.getEntity(loanId);
        Map<String, Object> vars = new HashMap<>();
        vars.put("borrowerName", loan.getBorrowerName());
        vars.put("bookTitle", loan.getBook().getTitle());
        vars.put("dueDate", DATE.format(loan.getDueDate()));
        mailSender.send(loan.getBorrowerEmail(),
                "Tu préstamo vence pronto: " + loan.getBook().getTitle(),
                "email/due-soon", vars);
    }

    public void sendBlockNotice(Long userId) {
        AppUser user = userService.getById(userId);
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", user.getName());
        vars.put("blockedUntil", user.getBlockedUntil() != null
                ? DATE_TIME.format(user.getBlockedUntil()) : "");
        mailSender.send(user.getEmail(),
                "Tu cuenta fue bloqueada temporalmente",
                "email/blocked", vars);
    }

    public void sendBookAvailable(Long reservationId) {
        Reservation reservation = reservationService.getById(reservationId);
        Map<String, Object> vars = new HashMap<>();
        vars.put("bookTitle", reservation.getBook().getTitle());
        vars.put("expiresAt", reservation.getExpiresAt() != null
                ? DATE_TIME.format(reservation.getExpiresAt()) : "");
        mailSender.send(reservation.getRequesterEmail(),
                "El libro que esperabas está disponible: " + reservation.getBook().getTitle(),
                "email/book-available", vars);
    }

    public void sendReservationCreated(Long reservationId) {
        Reservation reservation = reservationService.getById(reservationId);
        Map<String, Object> vars = new HashMap<>();
        vars.put("bookTitle", reservation.getBook().getTitle());
        vars.put("held", reservation.getExpiresAt() != null);
        vars.put("expiresAt", reservation.getExpiresAt() != null
                ? DATE_TIME.format(reservation.getExpiresAt()) : "");
        mailSender.send(reservation.getRequesterEmail(),
                "Reserva registrada: " + reservation.getBook().getTitle(),
                "email/reservation-created", vars);
    }

    public void sendReservationCancelled(Long reservationId) {
        Reservation reservation = reservationService.getById(reservationId);
        Map<String, Object> vars = new HashMap<>();
        vars.put("bookTitle", reservation.getBook().getTitle());
        mailSender.send(reservation.getRequesterEmail(),
                "Reserva cancelada: " + reservation.getBook().getTitle(),
                "email/reservation-cancelled", vars);
    }

    public void sendWelcome(Long userId) {
        AppUser user = userService.getById(userId);
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", user.getName());
        vars.put("loginLink", frontendProperties.baseUrl() + "/login");
        mailSender.send(user.getEmail(),
                "Bienvenido/a a la biblioteca",
                "email/welcome", vars);
    }

    public void sendActivation(Long userId) {
        AppUser user = userService.getById(userId);
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", user.getName());
        vars.put("activationLink", frontendProperties.baseUrl() + "/activate?token=" + user.getActivationToken());
        mailSender.send(user.getEmail(),
                "Activá tu cuenta en la biblioteca",
                "email/activation", vars);
    }
}
