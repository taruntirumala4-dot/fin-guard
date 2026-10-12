package com.finguard.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * User entity – represents a registered FinGuard user in SQL database.
 *
 * Roles:
 *   USER     – can view/create own transactions
 *   ANALYST  – can view transactions and fraud alerts
 *   ADMIN    – full access
 */
@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false, length = 191)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum Role {
        USER, ADMIN, ANALYST
    }

    public String getIdString() {
        return id != null ? String.valueOf(id) : null;
    }
}
