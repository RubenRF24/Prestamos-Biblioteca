package com.biblioteca.prestamosbiblioteca.notification.infra;

import com.biblioteca.prestamosbiblioteca.notification.domain.AppMailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Renderiza una plantilla Thymeleaf y envía el correo HTML resultante. */
@Component
public class ThymeleafMailSender {

    private static final Logger log = LoggerFactory.getLogger(ThymeleafMailSender.class);
    private static final Locale LOCALE = Locale.forLanguageTag("es");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppMailProperties properties;

    public ThymeleafMailSender(JavaMailSender mailSender,
                               TemplateEngine templateEngine,
                               AppMailProperties properties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.properties = properties;
    }

    public void send(String to, String subject, String template, Map<String, Object> variables) {
        Context context = new Context(LOCALE);
        context.setVariables(variables);
        String html = templateEngine.process(template, context);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Correo enviado a {} [{}]", to, subject);
        } catch (MessagingException ex) {
            log.error("No se pudo enviar el correo a {} [{}]: {}", to, subject, ex.getMessage());
        }
    }
}
