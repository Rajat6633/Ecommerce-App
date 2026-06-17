package com.ecommerce.auth.api.dto;

import com.ecommerce.auth.application.port.in.AuthenticationUseCase.AuthenticatedUser;
import com.ecommerce.auth.domain.model.Role;

import java.util.Set;
import java.util.UUID;

/** Public representation of a user. */
public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Set<Role> roles
) {
    public static UserResponse from(AuthenticatedUser u) {
        return new UserResponse(u.id(), u.email(), u.fullName(), u.roles());
    }
}
