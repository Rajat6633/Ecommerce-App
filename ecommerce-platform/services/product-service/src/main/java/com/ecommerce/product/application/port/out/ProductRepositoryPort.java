package com.ecommerce.product.application.port.out;

import com.ecommerce.product.application.model.PageResult;
import com.ecommerce.product.application.port.in.ProductSearchQuery;
import com.ecommerce.product.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for product persistence + search. */
public interface ProductRepositoryPort {

    boolean existsBySku(String sku);

    Product save(Product product);

    Optional<Product> findById(UUID id);

    void deleteById(UUID id);

    PageResult<Product> search(ProductSearchQuery query);

    /** All products, for a one-shot rebuild of the vector index. */
    List<Product> findAll();
}
