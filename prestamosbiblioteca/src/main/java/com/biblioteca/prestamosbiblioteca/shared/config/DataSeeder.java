package com.biblioteca.prestamosbiblioteca.shared.config;

import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;
import com.biblioteca.prestamosbiblioteca.user.domain.Role;
import com.biblioteca.prestamosbiblioteca.user.domain.UserStatus;
import com.biblioteca.prestamosbiblioteca.user.infra.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el usuario ADMIN de arranque si no existe. Idempotente: no deja hashes ni
 * credenciales en el repositorio; el password sale de la configuración (env en prod).
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeederProperties adminProperties;

    public DataSeeder(AppUserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      AdminSeederProperties adminProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(adminProperties.email())) {
            return;
        }
        AppUser admin = AppUser.builder()
                .name(adminProperties.name())
                .email(adminProperties.email())
                .passwordHash(passwordEncoder.encode(adminProperties.password()))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(admin);
        log.info("Usuario ADMIN de arranque creado: {}", adminProperties.email());
    }
}
