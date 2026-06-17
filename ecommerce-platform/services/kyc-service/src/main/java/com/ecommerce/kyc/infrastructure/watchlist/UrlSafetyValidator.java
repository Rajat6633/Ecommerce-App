package com.ecommerce.kyc.infrastructure.watchlist;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * SSRF guard for outbound watchlist-feed URLs. Validates that a feed URL is safe
 * to fetch before any HTTP call is made:
 *
 * <ul>
 *   <li>scheme MUST be {@code https};</li>
 *   <li>host must resolve to public unicast addresses only — loopback,
 *       link-local ({@code 169.254/16}, {@code fe80::/10}), site-local /
 *       RFC-1918 private ({@code 10/8}, {@code 172.16/12}, {@code 192.168/16}),
 *       any-local ({@code 0.0.0.0}, {@code ::}), multicast, and IPv6 ULA
 *       ({@code fc00::/7}) / {@code ::1} are all rejected;</li>
 *   <li>literal-IP hosts are rejected unless the literal is explicitly
 *       allow-listed;</li>
 *   <li>when an allow-list is configured (non-empty), the host must be a member.</li>
 * </ul>
 *
 * <p>The same validator is applied at startup (fail fast) and immediately before
 * each fetch, and can be used to re-validate redirect targets.
 */
public class UrlSafetyValidator {

    private final Set<String> allowedHosts;

    /**
     * @param allowedHosts case-insensitive host allow-list; when empty, any
     *                     publicly-resolving https host is permitted.
     */
    public UrlSafetyValidator(Collection<String> allowedHosts) {
        Set<String> normalised = new LinkedHashSet<>();
        if (allowedHosts != null) {
            for (String h : allowedHosts) {
                if (h != null && !h.isBlank()) {
                    normalised.add(h.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.allowedHosts = Set.copyOf(normalised);
    }

    /**
     * @throws UnsafeUrlException if the URL is malformed or resolves to a
     *                            disallowed scheme/host/address.
     */
    public void validate(String url) { // NOSONAR overridable for offline tests (mocked HTTP, no DNS)
        if (url == null || url.isBlank()) {
            throw new UnsafeUrlException("Feed URL is null/blank");
        }

        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new UnsafeUrlException("Malformed feed URL: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("https")) {
            throw new UnsafeUrlException("Feed URL scheme must be https: " + url);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeUrlException("Feed URL has no host: " + url);
        }
        String normalisedHost = host.toLowerCase(Locale.ROOT);

        boolean literalIp = isLiteralIp(host);
        if (!allowedHosts.isEmpty()) {
            if (!allowedHosts.contains(normalisedHost)) {
                throw new UnsafeUrlException("Feed host not in allow-list: " + host);
            }
        } else if (literalIp) {
            // Without an explicit allow-list, never fetch a raw IP literal.
            throw new UnsafeUrlException("Literal-IP feed hosts are not allowed: " + host);
        }

        InetAddress[] resolved;
        try {
            resolved = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new UnsafeUrlException("Feed host did not resolve: " + host);
        }
        if (resolved.length == 0) {
            throw new UnsafeUrlException("Feed host resolved to no addresses: " + host);
        }
        for (InetAddress addr : resolved) {
            if (isDisallowed(addr)) {
                throw new UnsafeUrlException(
                        "Feed host resolves to a non-public address (" + addr.getHostAddress() + "): " + host);
            }
        }
    }

    private static boolean isDisallowed(InetAddress addr) {
        if (addr.isLoopbackAddress()      // 127/8, ::1
                || addr.isAnyLocalAddress()   // 0.0.0.0, ::
                || addr.isLinkLocalAddress()  // 169.254/16, fe80::/10
                || addr.isSiteLocalAddress()  // 10/8, 172.16/12, 192.168/16
                || addr.isMulticastAddress()) {
            return true;
        }
        byte[] b = addr.getAddress();
        // IPv6 Unique-Local Addresses fc00::/7 (fcxx / fdxx) — not flagged by the JDK helpers.
        if (b.length == 16 && (b[0] & 0xFE) == 0xFC) {
            return true;
        }
        // Explicit 169.254/16 guard (covers any case the JDK link-local check misses).
        return b.length == 4 && (b[0] & 0xFF) == 169 && (b[1] & 0xFF) == 254;
    }

    private static boolean isLiteralIp(String host) {
        // IPv6 literals arrive bracket-stripped from URI#getHost only when no brackets;
        // URI keeps brackets, so a leading '[' or a ':' marks an IPv6 literal.
        if (host.indexOf(':') >= 0 || host.startsWith("[")) {
            return true;
        }
        // IPv4 literal: four dot-separated decimal octets.
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }
            for (int i = 0; i < part.length(); i++) {
                if (!Character.isDigit(part.charAt(i))) {
                    return false;
                }
            }
            if (Integer.parseInt(part) > 255) {
                return false;
            }
        }
        return true;
    }

    /** Thrown when a feed URL is unsafe to fetch (SSRF guard). */
    public static class UnsafeUrlException extends RuntimeException {
        public UnsafeUrlException(String message) {
            super(message);
        }
    }
}
