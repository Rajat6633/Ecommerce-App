package com.ecommerce.product.application.port.in;

import com.ecommerce.product.domain.model.Category;

import java.util.List;
import java.util.UUID;

/** Inbound port for category operations. */
public interface CategoryUseCase {

    Category create(CreateCategoryCommand command);

    List<Category> findAll();

    Category getById(UUID id);

    record CreateCategoryCommand(String name, UUID parentId) {}
}
