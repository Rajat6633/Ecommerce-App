package com.ecommerce.auth.application.port.out;

import com.ecommerce.auth.domain.model.User;

/** Outbound port for issuing signed access tokens. */
public interface TokenProviderPort {

    /** Issue a signed JWT access token for the user. */
    String issueAccessToken(User user);

    /** Access-token time-to-live in seconds (advertised to clients). */
    long accessTokenTtlSeconds();
}
