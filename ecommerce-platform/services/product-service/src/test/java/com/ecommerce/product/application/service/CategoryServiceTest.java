package com.ecommerce.product.application.service;

import com.ecommerce.product.application.port.in.CategoryUseCase.CreateCategoryCommand;
import com.ecommerce.product.application.port.out.CategoryRepositoryPort;
import com.ecommerce.product.domain.exception.CategoryNotFoundException;
import com.ecommerce.product.domain.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    @Mock CategoryRepositoryPort categoryRepository;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, clock);
    }

    @Test
    void create_rootCategory_persists() {
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Category c = service.create(new CreateCategoryCommand("Books", null));

        assertThat(c.name()).isEqualTo("Books");
        assertThat(c.parentId()).isNull();
        verify(categoryRepository).save(any());
    }

    @Test
    void create_withUnknownParent_throwsNotFound() {
        UUID parent = UUID.randomUUID();
        when(categoryRepository.existsById(parent)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateCategoryCommand("Sub", parent)))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getById_unknown_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id)).isInstanceOf(CategoryNotFoundException.class);
    }
}
