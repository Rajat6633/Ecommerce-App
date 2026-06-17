package com.ecommerce.product.infrastructure.persistence;

import com.ecommerce.product.application.model.PageResult;
import com.ecommerce.product.application.port.in.ProductSearchQuery;
import com.ecommerce.product.application.port.out.ProductRepositoryPort;
import com.ecommerce.product.domain.model.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    /** Allow-list prevents arbitrary/injection sort fields. */
    private static final Set<String> ALLOWED_SORT = Set.of("name", "price", "createdAt", "sku");

    private final ProductJpaRepository repository;

    public ProductPersistenceAdapter(ProductJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsBySku(String sku) {
        return repository.existsBySku(sku);
    }

    @Override
    public Product save(Product product) {
        return repository.save(ProductEntity.fromDomain(product)).toDomain();
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return repository.findById(id).map(ProductEntity::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public List<Product> findAll() {
        return repository.findAll().stream().map(ProductEntity::toDomain).toList();
    }

    @Override
    public PageResult<Product> search(ProductSearchQuery q) {
        String sortField = ALLOWED_SORT.contains(q.sortBy()) ? q.sortBy() : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(q.sortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(q.page(), q.size(), Sort.by(dir, sortField));

        Page<ProductEntity> page = repository.findAll(buildSpecification(q), pageable);
        return new PageResult<>(
                page.getContent().stream().map(ProductEntity::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private Specification<ProductEntity> buildSpecification(ProductSearchQuery q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q.name() != null && !q.name().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + q.name().toLowerCase() + "%"));
            }
            if (q.categoryId() != null) {
                predicates.add(cb.equal(root.get("categoryId"), q.categoryId()));
            }
            if (q.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), q.minPrice()));
            }
            if (q.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), q.maxPrice()));
            }
            if (q.activeOnly()) {
                predicates.add(cb.isTrue(root.get("active")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
