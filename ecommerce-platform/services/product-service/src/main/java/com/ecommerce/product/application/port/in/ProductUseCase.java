package com.ecommerce.product.application.port.in;

import com.ecommerce.product.application.model.PageResult;
import com.ecommerce.product.domain.model.Product;

import java.math.BigDecimal;
import java.util.UUID;

/** Inbound port for product operations. */
public interface ProductUseCase {

    Product create(CreateProductCommand command);

    Product update(UUID id, UpdateProductCommand command);

    void delete(UUID id);

    Product getById(UUID id);

    PageResult<Product> search(ProductSearchQuery query);

    record CreateProductCommand(String sku, String name, String description,
                                BigDecimal price, String currency, UUID categoryId) {}

    record UpdateProductCommand(String name, String description, BigDecimal price,
                                String currency, UUID categoryId, boolean active) {}
}
