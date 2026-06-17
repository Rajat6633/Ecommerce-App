package com.ecommerce.cart.domain.exception;

/** product-service could not be reached (circuit open / timeout). -> HTTP 503. */
public class ProductServiceUnavailableException extends CartException {
    public ProductServiceUnavailableException() {
        super("Product service is currently unavailable; please retry shortly");
    }
}
