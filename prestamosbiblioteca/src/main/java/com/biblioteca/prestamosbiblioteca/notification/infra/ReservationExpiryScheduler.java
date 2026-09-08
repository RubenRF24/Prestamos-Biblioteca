package com.biblioteca.prestamosbiblioteca.notification.infra;

import com.biblioteca.prestamosbiblioteca.reservation.domain.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Expira las reservas cuya ventana de exclusividad venció y promueve al siguiente de la fila. */
@Component
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final ReservationService reservationService;

    public ReservationExpiryScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(cron = "${app.reservations.expiry-cron:0 0 * * * *}")
    public void expireHolds() {
        int expired = reservationService.expireHolds();
        if (expired > 0) {
            log.info("Reservas expiradas y promovidas: {}", expired);
        }
    }
}
