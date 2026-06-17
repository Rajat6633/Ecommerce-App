package com.ecommerce.auth.application.port.out;

import com.ecommerce.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/** Outbound port for user persistence. */
public interface UserRepositoryPort {

    boolean existsByEmail(String email);

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);
}
