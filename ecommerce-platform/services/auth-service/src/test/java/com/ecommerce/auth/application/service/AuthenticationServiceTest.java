package com.ecommerce.auth.application.service;

import com.ecommerce.auth.application.port.in.AuthenticationUseCase.LoginCommand;
import com.ecommerce.auth.application.port.in.AuthenticationUseCase.RefreshCommand;
import com.ecommerce.auth.application.port.in.AuthenticationUseCase.RegisterCommand;
import com.ecommerce.auth.application.port.in.AuthenticationUseCase.TokenPair;
import com.ecommerce.auth.application.port.out.PasswordHasherPort;
import com.ecommerce.auth.application.port.out.RefreshTokenFactoryPort;
import com.ecommerce.auth.application.port.out.RefreshTokenRepositoryPort;
import com.ecommerce.auth.application.port.out.TokenProviderPort;
import com.ecommerce.auth.application.port.out.UserEventPublisherPort;
import com.ecommerce.auth.application.port.out.UserRepositoryPort;
import com.ecommerce.auth.domain.exception.EmailAlreadyExistsException;
import com.ecommerce.auth.domain.exception.InvalidCredentialsException;
import com.ecommerce.auth.domain.exception.InvalidRefreshTokenException;
import com.ecommerce.auth.domain.exception.UserNotFoundException;
import com.ecommerce.auth.domain.model.RefreshToken;
import com.ecommerce.auth.domain.model.Role;
import com.ecommerce.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final long REFRESH_TTL = 604_800L;
    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    @Mock UserRepositoryPort userRepository;
    @Mock RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock TokenProviderPort tokenProvider;
    @Mock PasswordHasherPort passwordHasher;
    @Mock RefreshTokenFactoryPort refreshTokenFactory;
    @Mock UserEventPublisherPort userEventPublisher;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private AuthenticationService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(userRepository, refreshTokenRepository, tokenProvider,
                passwordHasher, refreshTokenFactory, userEventPublisher, clock, REFRESH_TTL);
    }

    private User existingUser() {
        return new User(userId, "a@b.com", "HASH", "Alice", true,
                Set.of(Role.CUSTOMER), NOW, NOW);
    }

    @Test
    void register_normalizesEmail_andAssignsCustomerRole() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn("HASH");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.register(new RegisterCommand("A@B.com ", "password123", "Alice"));

        assertThat(result.email()).isEqualTo("a@b.com");
        assertThat(result.roles()).containsExactly(Role.CUSTOMER);
        verify(userRepository).save(any());
    }

    @Test
    void register_publishesUserRegisteredEvent_withPersistedIdAndEmail() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn("HASH");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.register(new RegisterCommand("A@B.com ", "password123", "Alice"));

        ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(userEventPublisher).publishUserRegistered(idCaptor.capture(), emailCaptor.capture());
        assertThat(idCaptor.getValue()).isEqualTo(result.id());
        assertThat(emailCaptor.getValue()).isEqualTo("a@b.com");
    }

    @Test
    void register_duplicateEmail_throwsConflict_andDoesNotPublish() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterCommand("a@b.com", "password123", "Alice")))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
        verify(userEventPublisher, never()).publishUserRegistered(any(), any());
    }

    @Test
    void login_validCredentials_issuesTokenPairAndPersistsRefreshToken() {
        User user = existingUser();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password123", "HASH")).thenReturn(true);
        when(tokenProvider.issueAccessToken(user)).thenReturn("ACCESS");
        when(tokenProvider.accessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenFactory.generateRawToken()).thenReturn("RAW");
        when(refreshTokenFactory.hash("RAW")).thenReturn("RAWHASH");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TokenPair tokens = service.login(new LoginCommand("A@B.com", "password123"));

        assertThat(tokens.accessToken()).isEqualTo("ACCESS");
        assertThat(tokens.refreshToken()).isEqualTo("RAW");
        assertThat(tokens.expiresInSeconds()).isEqualTo(900L);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().tokenHash()).isEqualTo("RAWHASH");
        assertThat(captor.getValue().expiresAt()).isEqualTo(NOW.plusSeconds(REFRESH_TTL));
        assertThat(captor.getValue().revoked()).isFalse();
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(existingUser()));
        when(passwordHasher.matches("bad", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand("a@b.com", "bad")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_unknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("nobody@b.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand("nobody@b.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_activeToken_rotatesAndRevokesOld() {
        RefreshToken stored = new RefreshToken(UUID.randomUUID(), userId, "RAWHASH",
                NOW.plusSeconds(1000), false, NOW);
        when(refreshTokenFactory.hash("RAW")).thenReturn("RAWHASH");
        when(refreshTokenRepository.findByTokenHash("RAWHASH")).thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser()));
        when(tokenProvider.issueAccessToken(any())).thenReturn("ACCESS2");
        when(tokenProvider.accessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenFactory.generateRawToken()).thenReturn("RAW2");
        when(refreshTokenFactory.hash("RAW2")).thenReturn("RAWHASH2");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TokenPair tokens = service.refresh(new RefreshCommand("RAW"));

        verify(refreshTokenRepository).revoke(stored.id());
        assertThat(tokens.refreshToken()).isEqualTo("RAW2");
        assertThat(tokens.accessToken()).isEqualTo("ACCESS2");
    }

    @Test
    void refresh_revokedTokenPresented_triggersReuseDefenseAndFails() {
        RefreshToken revoked = new RefreshToken(UUID.randomUUID(), userId, "RAWHASH",
                NOW.plusSeconds(1000), true, NOW);
        when(refreshTokenFactory.hash("RAW")).thenReturn("RAWHASH");
        when(refreshTokenRepository.findByTokenHash("RAWHASH")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.refresh(new RefreshCommand("RAW")))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokenRepository).revokeAllForUser(userId);
    }

    @Test
    void refresh_unknownToken_throwsUnauthorized() {
        when(refreshTokenFactory.hash("RAW")).thenReturn("RAWHASH");
        when(refreshTokenRepository.findByTokenHash("RAWHASH")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(new RefreshCommand("RAW")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void logout_existingToken_isRevoked() {
        RefreshToken stored = new RefreshToken(UUID.randomUUID(), userId, "RAWHASH",
                NOW.plusSeconds(1000), false, NOW);
        when(refreshTokenFactory.hash("RAW")).thenReturn("RAWHASH");
        when(refreshTokenRepository.findByTokenHash("RAWHASH")).thenReturn(Optional.of(stored));

        service.logout(new RefreshCommand("RAW"));

        verify(refreshTokenRepository).revoke(stored.id());
    }

    @Test
    void currentUser_unknownId_throwsNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.currentUser(userId))
                .isInstanceOf(UserNotFoundException.class);
    }
}
