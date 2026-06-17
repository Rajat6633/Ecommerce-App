package com.ecommerce.auth.application.port.in;

import com.ecommerce.auth.domain.model.Role;

import java.util.Set;
import java.util.UUID;

/**
 * Inbound port — the application's authentication API. Adapters (REST) depend
 * on this interface, not on the implementation (Clean Architecture).
 */
public interface AuthenticationUseCase {

    AuthenticatedUser register(RegisterCommand command);

    TokenPair login(LoginCommand command);

    TokenPair refresh(RefreshCommand command);

    void logout(RefreshCommand command);

    AuthenticatedUser currentUser(UUID userId);

    // ---- Commands ----
    record RegisterCommand(String email, String rawPassword, String fullName) {}

    record LoginCommand(String email, String rawPassword) {}

    record RefreshCommand(String refreshToken) {}

    // ---- Results ----
    record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {}

    record AuthenticatedUser(UUID id, String email, String fullName, Set<Role> roles) {}
}
