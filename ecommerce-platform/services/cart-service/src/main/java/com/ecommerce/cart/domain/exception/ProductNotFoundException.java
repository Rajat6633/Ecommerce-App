package com.ecommerce.cart.domain.exception;

/** Product does not exist in the catalog. -> HTTP 404. */
public class ProductNotFoundException extends CartException {
    public ProductNotFoundException(String productId) {
        super("Product not found: " + productId);
    }
}
