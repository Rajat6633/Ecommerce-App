package com.ecommerce.cart.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port to the product catalog (product-service). The implementation
 * is resilient (circuit breaker + fallback); a downstream outage surfaces as a
 * {@code ProductServiceUnavailableException}, not a raw error.
 */
public interface ProductCatalogPort {

    Optional<ProductInfo> findProduct(UUID productId);

    record ProductInfo(UUID productId, BigDecimal price, String currency, boolean active) {
    }
}
