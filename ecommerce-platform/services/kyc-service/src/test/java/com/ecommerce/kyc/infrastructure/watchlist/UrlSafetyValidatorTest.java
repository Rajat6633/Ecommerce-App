package com.ecommerce.kyc.infrastructure.watchlist;

import com.ecommerce.kyc.infrastructure.watchlist.UrlSafetyValidator.UnsafeUrlException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlSafetyValidatorTest {

    private final UrlSafetyValidator unrestricted = new UrlSafetyValidator(List.of());

    @Test
    void rejectsNonHttpsScheme() {
        assertThatThrownBy(() -> unrestricted.validate("http://www.treasury.gov/sdn.csv"))
                .isInstanceOf(UnsafeUrlException.class)
                .hasMessageContaining("https");
    }

    @Test
    void rejectsNullOrBlank() {
        assertThatThrownBy(() -> unrestricted.validate(null)).isInstanceOf(UnsafeUrlException.class);
        assertThatThrownBy(() -> unrestricted.validate("   ")).isInstanceOf(UnsafeUrlException.class);
    }

    @Test
    void rejectsMalformedUrl() {
        assertThatThrownBy(() -> unrestricted.validate("https://exa mple.com"))
                .isInstanceOf(UnsafeUrlException.class);
    }

    @Test
    void rejectsLoopbackLiteral() {
        assertThatThrownBy(() -> unrestricted.validate("https://127.0.0.1/sdn.csv"))
                .isInstanceOf(UnsafeUrlException.class);
    }

    @Test
    void rejectsPrivateRfc1918Literal() {
        assertThatThrownBy(() -> unrestricted.validate("https://10.0.0.5/sdn.csv"))
                .isInstanceOf(UnsafeUrlException.class);
        assertThatThrownBy(() -> unrestricted.validate("https://192.168.1.1/sdn.csv"))
                .isInstanceOf(UnsafeUrlException.class);
        assertThatThrownBy(() -> unrestricted.validate("https://172.16.0.1/sdn.csv"))
                .isInstanceOf(UnsafeUrlException.class);
    }

    @Test
    void rejectsLinkLocalMetadataAddress() {
        // AWS/GCP metadata endpoint — a classic SSRF target.
        assertThatThrownBy(() -> unrestricted.validate("https://169.254.169.254/latest/meta-data/"))
                .isInstanceOf(UnsafeUrlException.class);
    }

    @Test
    void rejectsIpv6LoopbackLiteral() {
        assertThatThrownBy(() -> unrestricted.validate("https://[::1]/sdn.csv"))
                .isInstanceOf(UnsafeUrlException.class);
    }

    @Test
    void rejectsLiteralIpWhenNoAllowList() {
        // A public-routable literal IP is still rejected without an explicit allow-list.
        assertThatThrownBy(() -> unrestricted.validate("https://8.8.8.8/sdn.csv"))
                .isInstanceOf(UnsafeUrlException.class)
                .hasMessageContaining("Literal-IP");
    }

    @Test
    void rejectsHostOutsideAllowList() {
        UrlSafetyValidator restricted = new UrlSafetyValidator(List.of("www.treasury.gov"));
        assertThatThrownBy(() -> restricted.validate("https://evil.example.com/sdn.csv"))
                .isInstanceOf(UnsafeUrlException.class)
                .hasMessageContaining("allow-list");
    }

    @Test
    void allowsConfiguredPublicHost() {
        // www.treasury.gov resolves to public addresses; skip if offline (no DNS).
        UrlSafetyValidator restricted = new UrlSafetyValidator(List.of("www.treasury.gov"));
        try {
            java.net.InetAddress.getByName("www.treasury.gov");
        } catch (java.net.UnknownHostException offline) {
            return; // no network in CI sandbox — nothing to assert
        }
        assertThatCode(() -> restricted.validate("https://www.treasury.gov/ofac/downloads/sdn.csv"))
                .doesNotThrowAnyException();
    }
}
