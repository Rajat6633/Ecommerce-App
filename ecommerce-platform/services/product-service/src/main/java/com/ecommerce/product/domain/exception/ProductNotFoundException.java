package com.ecommerce.product.domain.exception;

/** -> HTTP 404. */
public class ProductNotFoundException extends ProductException {
    public ProductNotFoundException(String id) {
        super("Product not found: " + id);
    }
}
