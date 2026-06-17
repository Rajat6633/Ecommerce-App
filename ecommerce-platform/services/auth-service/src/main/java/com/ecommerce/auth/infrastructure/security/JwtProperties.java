package com.ecommerce.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * JWT configuration. Keys are optional: when locations are absent an ephemeral
 * RSA keypair is generated at startup (intended for local/test only).
 */
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    /** Token issuer claim. */
    private String issuer = "auth-service";

    /** Token audience (aud) claim; resource servers validate this. */
    private String audience = "ecommerce-services";

    /** Access-token lifetime in seconds (default 15 min). */
    private long accessTokenTtlSeconds = 900;

    /** PEM (PKCS#8) RSA private key location; null/blank -> ephemeral key. */
    private Resource privateKeyLocation;

    /** PEM (X.509) RSA public key location; null/blank -> ephemeral key. */
    private Resource publicKeyLocation;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public Resource getPrivateKeyLocation() {
        return privateKeyLocation;
    }

    public void setPrivateKeyLocation(Resource privateKeyLocation) {
        this.privateKeyLocation = privateKeyLocation;
    }

    public Resource getPublicKeyLocation() {
        return publicKeyLocation;
    }

    public void setPublicKeyLocation(Resource publicKeyLocation) {
        this.publicKeyLocation = publicKeyLocation;
    }
}
