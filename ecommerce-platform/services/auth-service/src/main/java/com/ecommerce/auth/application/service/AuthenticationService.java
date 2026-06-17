package com.ecommerce.auth.application.service;

import com.ecommerce.auth.application.port.in.AuthenticationUseCase;
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
import com.ecommerce.auth.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Application service orchestrating registration, authentication, and
 * refresh-token rotation. Pure orchestration — all I/O is behind ports.
 */
@Service
public class AuthenticationService implements AuthenticationUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final TokenProviderPort tokenProvider;
    private final PasswordHasherPort passwordHasher;
    private final RefreshTokenFactoryPort refreshTokenFactory;
    private final UserEventPublisherPort userEventPublisher;
    private final Clock clock;
    private final long refreshTtlSeconds;

    public AuthenticationService(UserRepositoryPort userRepository,
                                 RefreshTokenRepositoryPort refreshTokenRepository,
                                 TokenProviderPort tokenProvider,
                                 PasswordHasherPort passwordHasher,
                                 RefreshTokenFactoryPort refreshTokenFactory,
                                 UserEventPublisherPort userEventPublisher,
                                 Clock clock,
                                 @Value("${auth.refresh-token-ttl-seconds:604800}") long refreshTtlSeconds) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.passwordHasher = passwordHasher;
        this.refreshTokenFactory = refreshTokenFactory;
        this.userEventPublisher = userEventPublisher;
        this.clock = clock;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    @Override
    @Transactional
    public AuthenticatedUser register(RegisterCommand command) {
        String email = normalize(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        Instant now = clock.instant();
        User user = User.newCustomer(
                UUID.randomUUID(),
                email,
                passwordHasher.hash(command.rawPassword()),
                command.fullName(),
                now);
        User saved = userRepository.save(user);
        // Commit-before-publish: the adapter defers the actual Kafka send until
        // AFTER_COMMIT, so the user.registered event is only emitted once the
        // registration transaction has durably committed. A publish failure is
        // swallowed by the adapter's fallback and never breaks registration.
        userEventPublisher.publishUserRegistered(saved.id(), saved.email());
        log.info("User registered userId={}", saved.id());
        return toAuthenticatedUser(saved);
    }

    @Override
    @Transactional
    public TokenPair login(LoginCommand command) {
        String email = normalize(command.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new); // avoid user enumeration
        if (!user.enabled() || !passwordHasher.matches(command.rawPassword(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        log.info("Login success userId={}", user.id());
        return issueTokens(user);
    }

    @Override
    @Transactional
    public TokenPair refresh(RefreshCommand command) {
        String hash = refreshTokenFactory.hash(command.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = clock.instant();
        if (!stored.isActive(now)) {
            // A revoked-but-presented token signals possible theft/replay:
            // defensively revoke the whole family for that user.
            if (stored.revoked()) {
                log.warn("Refresh token reuse detected userId={} — revoking all tokens", stored.userId());
                refreshTokenRepository.revokeAllForUser(stored.userId());
            }
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(stored.userId())
                .orElseThrow(InvalidRefreshTokenException::new);

        // Rotation: revoke the presented token, issue a brand-new pair.
        refreshTokenRepository.revoke(stored.id());
        log.info("Refresh token rotated userId={}", user.id());
        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(RefreshCommand command) {
        String hash = refreshTokenFactory.hash(command.refreshToken());
        // Idempotent: silently succeed whether or not the token exists.
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> refreshTokenRepository.revoke(token.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser currentUser(UUID userId) {
        return userRepository.findById(userId)
                .map(this::toAuthenticatedUser)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
    }

    // ---- helpers ----

    private TokenPair issueTokens(User user) {
        String accessToken = tokenProvider.issueAccessToken(user);
        String rawRefresh = refreshTokenFactory.generateRawToken();
        Instant now = clock.instant();
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                user.id(),
                refreshTokenFactory.hash(rawRefresh),
                now.plusSeconds(refreshTtlSeconds),
                false,
                now);
        refreshTokenRepository.save(refreshToken);
        return new TokenPair(accessToken, rawRefresh, tokenProvider.accessTokenTtlSeconds());
    }

    private AuthenticatedUser toAuthenticatedUser(User user) {
        return new AuthenticatedUser(user.id(), user.email(), user.fullName(), user.roles());
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
