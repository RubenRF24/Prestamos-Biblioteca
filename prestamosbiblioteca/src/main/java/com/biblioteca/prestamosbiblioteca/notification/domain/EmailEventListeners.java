package com.biblioteca.prestamosbiblioteca.notification.domain;

import com.biblioteca.prestamosbiblioteca.loan.domain.LoanCreatedEvent;
import com.biblioteca.prestamosbiblioteca.reservation.domain.BookAvailableEvent;
import com.biblioteca.prestamosbiblioteca.user.domain.ActivationRequestedEvent;
import com.biblioteca.prestamosbiblioteca.user.domain.UserBlockedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha los eventos de dominio y dispara los correos de forma asíncrona DESPUÉS del commit,
 * para no bloquear la petición HTTP que los originó.
 */
@Component
public class EmailEventListeners {

    private final NotificationService notificationService;

    public EmailEventListeners(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanCreated(LoanCreatedEvent event) {
        notificationService.sendLoanConfirmation(event.loanId());
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserBlocked(UserBlockedEvent event) {
        notificationService.sendBlockNotice(event.userId());
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookAvailable(BookAvailableEvent event) {
        notificationService.sendBookAvailable(event.reservationId());
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onActivationRequested(ActivationRequestedEvent event) {
        notificationService.sendActivation(event.userId());
    }
}
