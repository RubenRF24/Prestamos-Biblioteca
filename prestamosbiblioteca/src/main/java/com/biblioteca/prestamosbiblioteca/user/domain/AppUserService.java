package com.biblioteca.prestamosbiblioteca.user.domain;

import com.biblioteca.prestamosbiblioteca.shared.exception.EmailAlreadyUsedException;
import com.biblioteca.prestamosbiblioteca.shared.exception.InvalidActivationException;
import com.biblioteca.prestamosbiblioteca.shared.exception.ResourceNotFoundException;
import com.biblioteca.prestamosbiblioteca.user.infra.AppUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserService {

    /** Vigencia del token de activación de una cuenta provisional. */
    private static final Duration ACTIVATION_TTL = Duration.ofDays(7);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    public AppUserService(AppUserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    /** Auto-registro público: crea una cuenta ACTIVE con rol USUARIO (lector). */
    @Transactional
    public AppUser register(String name, String email, String rawPassword) {
        return createActive(name, email, rawPassword, Role.USUARIO);
    }

    /** Alta de cuenta por parte de un ADMIN (staff). */
    @Transactional
    public AppUser createByAdmin(String name, String email, String rawPassword, Role role) {
        return createActive(name, email, rawPassword, role == null ? Role.BIBLIOTECARIO : role);
    }

    private AppUser createActive(String name, String email, String rawPassword, Role role) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        AppUser user = AppUser.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
        AppUser saved = userRepository.save(user);
        events.publishEvent(new UserRegisteredEvent(saved.getId()));
        return saved;
    }

    /**
     * Busca la cuenta por email; si no existe, la crea como provisional (PENDING_ACTIVATION)
     * y publica el evento para enviarle el correo de activación. Usado al prestar a un tercero.
     */
    @Transactional
    public AppUser findOrProvision(String name, String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> provision(name, email));
    }

    private AppUser provision(String name, String email) {
        AppUser user = AppUser.builder()
                .name(name)
                .email(email)
                .passwordHash(null)
                .role(Role.USUARIO)
                .status(UserStatus.PENDING_ACTIVATION)
                .activationToken(UUID.randomUUID().toString())
                .activationTokenExpiresAt(Instant.now().plus(ACTIVATION_TTL))
                .build();
        AppUser saved = userRepository.save(user);
        events.publishEvent(new ActivationRequestedEvent(saved.getId()));
        return saved;
    }

    /** Activa una cuenta provisional fijando su contraseña. */
    @Transactional
    public AppUser activate(String token, String rawPassword) {
        AppUser user = userRepository.findByActivationToken(token)
                .orElseThrow(() -> new InvalidActivationException("Token de activación inválido"));
        if (user.getActivationTokenExpiresAt() == null
                || user.getActivationTokenExpiresAt().isBefore(Instant.now())) {
            throw new InvalidActivationException("El token de activación expiró");
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(UserStatus.ACTIVE);
        user.setActivationToken(null);
        user.setActivationTokenExpiresAt(null);
        return userRepository.save(user);
    }

    public boolean isActivationTokenValid(String token) {
        return userRepository.findByActivationToken(token)
                .filter(u -> u.getActivationTokenExpiresAt() != null
                        && u.getActivationTokenExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    /** Aplica el bloqueo temporal y publica el evento de aviso. */
    @Transactional
    public void applyBlock(AppUser user, Instant until) {
        user.setBlocked(true);
        user.setBlockedUntil(until);
        userRepository.save(user);
        events.publishEvent(new UserBlockedEvent(user.getId()));
    }

    /** Levanta el bloqueo automáticamente si la ventana ya venció. */
    @Transactional
    public void clearExpiredBlock(AppUser user) {
        if (user.isBlocked()
                && (user.getBlockedUntil() == null || !user.getBlockedUntil().isAfter(Instant.now()))) {
            user.setBlocked(false);
            user.setBlockedUntil(null);
            userRepository.save(user);
        }
    }

    @Transactional
    public AppUser unblock(Long userId) {
        AppUser user = getById(userId);
        user.setBlocked(false);
        user.setBlockedUntil(null);
        return userRepository.save(user);
    }

    public AppUser getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));
    }

    public AppUser getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }

    public List<AppUser> findBlocked() {
        return userRepository.findByBlockedTrue();
    }
}
