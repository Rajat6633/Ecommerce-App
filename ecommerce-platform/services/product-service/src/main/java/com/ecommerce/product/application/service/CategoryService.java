package com.ecommerce.product.application.service;

import com.ecommerce.product.application.port.in.CategoryUseCase;
import com.ecommerce.product.application.port.out.CategoryRepositoryPort;
import com.ecommerce.product.domain.exception.CategoryNotFoundException;
import com.ecommerce.product.domain.model.Category;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService implements CategoryUseCase {

    private final CategoryRepositoryPort categoryRepository;
    private final Clock clock;

    public CategoryService(CategoryRepositoryPort categoryRepository, Clock clock) {
        this.categoryRepository = categoryRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Category create(CreateCategoryCommand command) {
        if (command.parentId() != null && !categoryRepository.existsById(command.parentId())) {
            throw new CategoryNotFoundException(command.parentId().toString());
        }
        Category category = new Category(UUID.randomUUID(), command.name(),
                command.parentId(), clock.instant());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Category getById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));
    }
}
