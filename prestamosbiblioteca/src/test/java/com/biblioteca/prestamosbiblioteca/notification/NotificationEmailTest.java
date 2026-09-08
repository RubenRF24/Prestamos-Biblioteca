package com.biblioteca.prestamosbiblioteca.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.biblioteca.prestamosbiblioteca.TestcontainersConfiguration;
import com.biblioteca.prestamosbiblioteca.book.domain.Book;
import com.biblioteca.prestamosbiblioteca.book.domain.BookStatus;
import com.biblioteca.prestamosbiblioteca.book.infra.BookRepository;
import com.biblioteca.prestamosbiblioteca.loan.domain.Loan;
import com.biblioteca.prestamosbiblioteca.loan.domain.LoanService;
import com.biblioteca.prestamosbiblioteca.notification.domain.NotificationService;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;
import com.biblioteca.prestamosbiblioteca.user.domain.AppUserService;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el correo de confirmación de préstamo realmente se envía (SMTP embebido GreenMail),
 * con el asunto y destinatario correctos. El envío se invoca de forma directa para que la prueba
 * sea determinística; el disparo asíncrono por evento se valida por separado (a mano contra MailHog).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class NotificationEmailTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private LoanService loanService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AppUserService userService;
    @Autowired
    private BookRepository bookRepository;

    @Test
    void confirmacionDePrestamo_seEnviaConAsuntoYDestinatario() throws Exception {
        AppUser user = userService.register("Ana", "ana.confirm@mail.com", "password123");
        Book book = bookRepository.save(Book.builder()
                .title("Refactoring").author("Fowler").isbn("ISBN-IT-1")
                .status(BookStatus.DISPONIBLE).build());
        Loan loan = loanService.create(book.getId(), null, null, user);

        notificationService.sendLoanConfirmation(loan.getId());

        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        // Puede haber otros correos (p. ej. bienvenida del registro); buscamos la confirmación.
        MimeMessage confirmation = null;
        for (MimeMessage message : greenMail.getReceivedMessages()) {
            if (message.getSubject() != null && message.getSubject().contains("Confirmación de préstamo")) {
                confirmation = message;
                break;
            }
        }
        assertThat(confirmation).as("correo de confirmación de préstamo").isNotNull();
        assertThat(confirmation.getSubject()).contains("Refactoring");
        assertThat(confirmation.getAllRecipients()[0].toString()).isEqualTo("ana.confirm@mail.com");
    }
}
