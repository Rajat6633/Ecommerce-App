package com.ecommerce.auth.infrastructure.persistence;

import com.ecommerce.auth.domain.model.Role;
import com.ecommerce.auth.domain.model.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** JPA mapping for users + their roles (user_roles element collection). */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String fullName;

    @Column(nullable = false)
    private boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected UserEntity() { // for JPA
    }

    public static UserEntity fromDomain(User u) {
        UserEntity e = new UserEntity();
        e.id = u.id();
        e.email = u.email();
        e.passwordHash = u.passwordHash();
        e.fullName = u.fullName();
        e.enabled = u.enabled();
        e.roles = u.roles().isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(u.roles());
        e.createdAt = u.createdAt();
        e.updatedAt = u.updatedAt();
        return e;
    }

    public User toDomain() {
        return new User(id, email, passwordHash, fullName, enabled, Set.copyOf(roles), createdAt, updatedAt);
    }

    public UUID getId() {
        return id;
    }
}
