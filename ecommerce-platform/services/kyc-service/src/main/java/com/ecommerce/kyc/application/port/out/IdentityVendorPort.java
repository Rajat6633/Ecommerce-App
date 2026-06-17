package com.ecommerce.kyc.application.port.out;

import java.util.UUID;

/**
 * Specialist identity-verification vendor (Onfido/Jumio) for liveness / face
 * match. Optional — gated by {@code kyc.vendor.enabled}; a simulated adapter is
 * active by default so local/dev/test need no vendor key. Fails closed.
 */
public interface IdentityVendorPort {

    VendorCheck verify(UUID userId);

    /** Vendor outcome. {@code passed=false} on a fail-closed fallback. */
    record VendorCheck(boolean passed, String reference, String detail) {
        public static VendorCheck failClosed(String detail) {
            return new VendorCheck(false, null, detail);
        }
    }
}
