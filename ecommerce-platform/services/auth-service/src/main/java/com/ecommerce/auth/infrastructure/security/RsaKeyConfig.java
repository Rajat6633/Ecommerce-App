package com.ecommerce.auth.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Builds the RS256 signing material. Loads a PEM keypair when configured,
 * otherwise generates an ephemeral 2048-bit keypair (local/test). Exposes the
 * {@link JwtEncoder} (sign) and {@link JwtDecoder} (verify) beans.
 */
@Configuration
public class RsaKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyConfig.class);
    static final String KEY_ID = "auth-key";

    @Bean
    public RSAKey rsaKey(JwtProperties props) {
        Resource priv = props.getPrivateKeyLocation();
        Resource pub = props.getPublicKeyLocation();
        if (priv != null && priv.exists() && pub != null && pub.exists()) {
            log.info("Loading RSA JWT keypair from configured PEM locations");
            return loadFromPem(priv, pub);
        }
        log.warn("No RSA key locations configured — generating an EPHEMERAL keypair "
                + "(acceptable for local/test only; configure auth.jwt.*-key-location in prod)");
        return generateEphemeral();
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaKey) {
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwks);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) {
        try {
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build JwtDecoder from RSA public key", e);
        }
    }

    private RSAKey loadFromPem(Resource privateKey, Resource publicKey) {
        try (InputStream privIn = privateKey.getInputStream();
             InputStream pubIn = publicKey.getInputStream()) {
            RSAPrivateKey rsaPrivate = RsaKeyConverters.pkcs8().convert(privIn);
            RSAPublicKey rsaPublic = RsaKeyConverters.x509().convert(pubIn);
            return new RSAKey.Builder(rsaPublic).privateKey(rsaPrivate).keyID(KEY_ID).build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read RSA PEM key resources", e);
        }
    }

    private RSAKey generateEphemeral() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair kp = gen.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                    .privateKey((RSAPrivateKey) kp.getPrivate())
                    .keyID(KEY_ID)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate ephemeral RSA keypair", e);
        }
    }
}
