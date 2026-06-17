package com.ecommerce.product.domain.exception;

/** Base type for product-domain errors. */
public abstract class ProductException extends RuntimeException {
    protected ProductException(String message) {
        super(message);
    }
}
