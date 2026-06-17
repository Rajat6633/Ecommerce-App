package com.ecommerce.product.infrastructure.persistence;

import com.ecommerce.product.application.port.out.CategoryRepositoryPort;
import com.ecommerce.product.domain.model.Category;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CategoryPersistenceAdapter implements CategoryRepositoryPort {

    private final CategoryJpaRepository repository;

    public CategoryPersistenceAdapter(CategoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Category save(Category category) {
        return repository.save(CategoryEntity.fromDomain(category)).toDomain();
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return repository.findById(id).map(CategoryEntity::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return repository.findAll().stream().map(CategoryEntity::toDomain).toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }
}
