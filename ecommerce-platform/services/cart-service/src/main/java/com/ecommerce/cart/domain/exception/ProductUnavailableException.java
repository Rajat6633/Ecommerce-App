package com.ecommerce.cart.domain.exception;

/** Product exists but is inactive/unpurchasable. -> HTTP 409. */
public class ProductUnavailableException extends CartException {
    public ProductUnavailableException(String productId) {
        super("Product is not available for purchase: " + productId);
    }
}
