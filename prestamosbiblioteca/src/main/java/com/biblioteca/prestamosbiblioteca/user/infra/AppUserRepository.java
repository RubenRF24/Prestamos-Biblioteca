package com.biblioteca.prestamosbiblioteca.user.infra;

import com.biblioteca.prestamosbiblioteca.user.domain.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<AppUser> findByActivationToken(String activationToken);

    List<AppUser> findByBlockedTrue();
}
