package com.ecommerce.auth.infrastructure.security;

import com.ecommerce.auth.application.port.out.TokenProviderPort;
import com.ecommerce.auth.domain.model.Role;
import com.ecommerce.auth.domain.model.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Issues RS256-signed JWT access tokens. */
@Component
public class JwtTokenProvider implements TokenProviderPort {

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final Clock clock;

    public JwtTokenProvider(JwtEncoder encoder, JwtProperties props, Clock clock) {
        this.encoder = encoder;
        this.props = props;
        this.clock = clock;
    }

    @Override
    public String issueAccessToken(User user) {
        Instant now = clock.instant();
        List<String> roles = user.roles().stream().map(Role::name).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.getIssuer())
                .audience(List.of(props.getAudience()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(props.getAccessTokenTtlSeconds()))
                .subject(user.id().toString())
                .id(UUID.randomUUID().toString())
                .claim("email", user.email())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(RsaKeyConfig.KEY_ID).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    public long accessTokenTtlSeconds() {
        return props.getAccessTokenTtlSeconds();
    }
}
