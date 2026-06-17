package com.ecommerce.product.application.service;

import com.ecommerce.product.application.model.PageResult;
import com.ecommerce.product.application.port.in.ProductSearchQuery;
import com.ecommerce.product.application.port.in.ProductUseCase;
import com.ecommerce.product.application.port.out.CategoryRepositoryPort;
import com.ecommerce.product.application.port.out.ProductIndexPort;
import com.ecommerce.product.application.port.out.ProductRepositoryPort;
import com.ecommerce.product.domain.exception.CategoryNotFoundException;
import com.ecommerce.product.domain.exception.DuplicateSkuException;
import com.ecommerce.product.domain.exception.ProductNotFoundException;
import com.ecommerce.product.domain.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class ProductService implements ProductUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepositoryPort productRepository;
    private final CategoryRepositoryPort categoryRepository;
    private final ProductIndexPort productIndex;
    private final Clock clock;

    public ProductService(ProductRepositoryPort productRepository,
                          CategoryRepositoryPort categoryRepository,
                          ProductIndexPort productIndex,
                          Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productIndex = productIndex;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Product create(CreateProductCommand command) {
        if (productRepository.existsBySku(command.sku())) {
            throw new DuplicateSkuException(command.sku());
        }
        requireCategory(command.categoryId());
        Product product = Product.create(UUID.randomUUID(), command.sku(), command.name(),
                command.description(), command.price(), command.currency(),
                command.categoryId(), clock.instant());
        Product saved = productRepository.save(product);
        reindex(saved);
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public Product update(UUID id, UpdateProductCommand command) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id.toString()));
        requireCategory(command.categoryId());
        Product updated = existing.withUpdates(command.name(), command.description(),
                command.price(), command.currency(), command.categoryId(),
                command.active(), clock.instant());
        Product saved = productRepository.save(updated);
        reindex(saved);
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void delete(UUID id) {
        if (productRepository.findById(id).isEmpty()) {
            throw new ProductNotFoundException(id.toString());
        }
        productRepository.deleteById(id);
        try {
            productIndex.remove(id);
        } catch (RuntimeException e) {
            log.warn("Failed to remove product {} from vector index (continuing): {}", id, e.toString());
        }
    }

    /**
     * Fail-soft indexing: the index adapter is Resilience4j-wrapped and already
     * swallows model/store outages, but we also guard here so that an indexing
     * failure can never break product CRUD or roll back the persisted product.
     */
    private void reindex(Product product) {
        try {
            productIndex.index(product);
        } catch (RuntimeException e) {
            log.warn("Failed to index product {} (continuing): {}", product.id(), e.toString());
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public Product getById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Product> search(ProductSearchQuery query) {
        return productRepository.search(query);
    }

    private void requireCategory(UUID categoryId) {
        if (categoryId == null || !categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(String.valueOf(categoryId));
        }
    }
}
