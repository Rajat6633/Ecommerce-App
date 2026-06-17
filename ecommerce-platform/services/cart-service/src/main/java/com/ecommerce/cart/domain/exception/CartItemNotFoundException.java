package com.ecommerce.cart.domain.exception;

/** -> HTTP 404. */
public class CartItemNotFoundException extends CartException {
    public CartItemNotFoundException(String productId) {
        super("Cart item not found for product: " + productId);
    }
}
