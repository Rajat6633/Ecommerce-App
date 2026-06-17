package com.ecommerce.product.domain.exception;

/** -> HTTP 409. */
public class DuplicateSkuException extends ProductException {
    public DuplicateSkuException(String sku) {
        super("Product SKU already exists: " + sku);
    }
}
