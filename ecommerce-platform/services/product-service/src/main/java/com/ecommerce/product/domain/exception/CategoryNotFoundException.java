package com.ecommerce.product.domain.exception;

/** -> HTTP 404. */
public class CategoryNotFoundException extends ProductException {
    public CategoryNotFoundException(String id) {
        super("Category not found: " + id);
    }
}
