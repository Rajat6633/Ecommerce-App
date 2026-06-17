package com.ecommerce.cart.util;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;

/** Mints RS256 tokens for tests using the dev keypair the resource server trusts. */
public final class TestTokens {

    private static final NimbusJwtEncoder ENCODER = buildEncoder();

    private TestTokens() {
    }

    public static String mint(String subject, String... roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("auth-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject(subject)
                .claim("roles", List.of(roles))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("auth-key").build();
        return ENCODER.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static NimbusJwtEncoder buildEncoder() {
        try (InputStream priv = new ClassPathResource("keys/private.pem").getInputStream();
             InputStream pub = new ClassPathResource("keys/public.pem").getInputStream()) {
            RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(priv);
            RSAPublicKey publicKey = RsaKeyConverters.x509().convert(pub);
            RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("auth-key").build();
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build test JWT encoder", e);
        }
    }
}
