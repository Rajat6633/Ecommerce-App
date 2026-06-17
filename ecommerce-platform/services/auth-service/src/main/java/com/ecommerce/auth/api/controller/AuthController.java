package com.ecommerce.auth.api.controller;

import com.ecommerce.auth.api.dto.AuthResponse;
import com.ecommerce.auth.api.dto.LoginRequest;
import com.ecommerce.auth.api.dto.RefreshTokenRequest;
import com.ecommerce.auth.api.dto.RegisterRequest;
import com.ecommerce.auth.api.dto.UserResponse;
import com.ecommerce.auth.application.port.in.AuthenticationUseCase;
import com.ecommerce.auth.application.port.in.AuthenticationUseCase.LoginCommand;
import com.ecommerce.auth.application.port.in.AuthenticationUseCase.RefreshCommand;
import com.ecommerce.auth.application.port.in.AuthenticationUseCase.RegisterCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, login, token refresh, and profile")
public class AuthController {

    private final AuthenticationUseCase authentication;

    public AuthController(AuthenticationUseCase authentication) {
        this.authentication = authentication;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        var user = authentication.register(
                new RegisterCommand(request.email(), request.password(), request.fullName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive access + refresh tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        var tokens = authentication.login(new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok(AuthResponse.from(tokens));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token pair (rotation)")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var tokens = authentication.refresh(new RefreshCommand(request.refreshToken()));
        return ResponseEntity.ok(AuthResponse.from(tokens));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authentication.logout(new RefreshCommand(request.refreshToken()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(UserResponse.from(authentication.currentUser(userId)));
    }
}
