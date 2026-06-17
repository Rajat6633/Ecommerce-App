package com.ecommerce.kyc.infrastructure.vendor;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Real identity-verification vendor (Onfido/Jumio). Only instantiated when
 * {@code kyc.vendor.enabled=true}; the URL is supplied via config (K8s DNS or
 * external endpoint). Wrapped by {@link FeignIdentityVendorAdapter}.
 */
@FeignClient(name = "identity-vendor", url = "${kyc.vendor.url:http://identity-vendor}",
        configuration = VendorFeignConfig.class)
public interface IdentityVendorFeignClient {

    @PostMapping("/v1/checks")
    VendorCheckResponse runCheck(@RequestBody VendorCheckRequest request);

    record VendorCheckRequest(String applicantId) {
    }

    record VendorCheckResponse(boolean passed, String reference, String detail) {
    }
}
