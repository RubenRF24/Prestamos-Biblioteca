package com.biblioteca.prestamosbiblioteca.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /** Nulo mientras la cuenta esté PENDING_ACTIVATION. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "activation_token", length = 100)
    private String activationToken;

    @Column(name = "activation_token_expires_at")
    private Instant activationTokenExpiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean blocked = false;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /** Bloqueo efectivo sólo si está marcado y la ventana no venció. */
    public boolean isCurrentlyBlocked(Instant now) {
        return blocked && blockedUntil != null && blockedUntil.isAfter(now);
    }
}
