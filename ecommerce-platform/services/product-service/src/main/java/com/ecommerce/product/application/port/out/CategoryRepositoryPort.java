package com.ecommerce.product.application.port.out;

import com.ecommerce.product.domain.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for category persistence. */
public interface CategoryRepositoryPort {

    Category save(Category category);

    Optional<Category> findById(UUID id);

    List<Category> findAll();

    boolean existsById(UUID id);
}
