package com.biblioteca.prestamosbiblioteca.notification.infra;

import com.biblioteca.prestamosbiblioteca.loan.domain.Loan;
import com.biblioteca.prestamosbiblioteca.loan.domain.LoanService;
import com.biblioteca.prestamosbiblioteca.notification.domain.NotificationService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tarea diaria que avisa de los préstamos por vencer (uno o dos días antes). */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final LoanService loanService;
    private final NotificationService notificationService;

    public ReminderScheduler(LoanService loanService, NotificationService notificationService) {
        this.loanService = loanService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${app.loans.reminder-cron:0 0 8 * * *}")
    public void sendDueSoonReminders() {
        Instant now = Instant.now();
        Instant to = now.plus(Duration.ofDays(2));
        List<Loan> dueSoon = loanService.findDueSoonNeedingReminder(now, to);
        for (Loan loan : dueSoon) {
            notificationService.sendDueSoonReminder(loan.getId());
            loanService.markReminderSent(loan.getId());
        }
        if (!dueSoon.isEmpty()) {
            log.info("Recordatorios de vencimiento enviados: {}", dueSoon.size());
        }
    }
}
