package com.ecommerce.auth.api.dto;

import com.ecommerce.auth.application.port.in.AuthenticationUseCase.TokenPair;

/** Token response returned by login and refresh. */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static AuthResponse from(TokenPair pair) {
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), "Bearer", pair.expiresInSeconds());
    }
}
