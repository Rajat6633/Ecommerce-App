package com.ecommerce.product.application.service;

import com.ecommerce.product.application.model.PageResult;
import com.ecommerce.product.application.port.in.ProductSearchQuery;
import com.ecommerce.product.application.port.in.ProductUseCase.CreateProductCommand;
import com.ecommerce.product.application.port.in.ProductUseCase.UpdateProductCommand;
import com.ecommerce.product.application.port.out.CategoryRepositoryPort;
import com.ecommerce.product.application.port.out.ProductIndexPort;
import com.ecommerce.product.application.port.out.ProductRepositoryPort;
import com.ecommerce.product.domain.exception.CategoryNotFoundException;
import com.ecommerce.product.domain.exception.DuplicateSkuException;
import com.ecommerce.product.domain.exception.ProductNotFoundException;
import com.ecommerce.product.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    @Mock ProductRepositoryPort productRepository;
    @Mock CategoryRepositoryPort categoryRepository;
    @Mock ProductIndexPort productIndex;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private ProductService service;

    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, categoryRepository, productIndex, clock);
    }

    private CreateProductCommand cmd() {
        return new CreateProductCommand("SKU-1", "Widget", "desc",
                new BigDecimal("9.99"), "USD", categoryId);
    }

    @Test
    void create_validProduct_persistsActiveProduct_andIndexesIt() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Product p = service.create(cmd());

        assertThat(p.sku()).isEqualTo("SKU-1");
        assertThat(p.active()).isTrue();
        assertThat(p.createdAt()).isEqualTo(NOW);
        verify(productRepository).save(any());
        // Indexing happens on create (D1/D5 vector upsert).
        verify(productIndex).index(p);
    }

    @Test
    void create_indexingFailure_doesNotBreakSave() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        // Even if the vector index throws, the product save must still succeed.
        org.mockito.Mockito.doThrow(new RuntimeException("vector store down"))
                .when(productIndex).index(any());

        Product p = service.create(cmd());

        assertThat(p.sku()).isEqualTo("SKU-1");
        verify(productRepository).save(any());
        verify(productIndex).index(any());
    }

    @Test
    void update_validProduct_reindexesIt() {
        UUID id = UUID.randomUUID();
        Product existing = Product.create(id, "SKU-1", "old", "old desc",
                new BigDecimal("1.00"), "USD", categoryId, NOW);
        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Product p = service.update(id, new UpdateProductCommand(
                "new", "new desc", new BigDecimal("2.00"), "USD", categoryId, true));

        assertThat(p.name()).isEqualTo("new");
        verify(productIndex).index(p);
    }

    @Test
    void create_duplicateSku_throwsConflict() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd())).isInstanceOf(DuplicateSkuException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_unknownCategory_throwsNotFound() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(cmd())).isInstanceOf(CategoryNotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_unknownProduct_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id,
                new UpdateProductCommand("n", "d", new BigDecimal("1.00"), "USD", categoryId, true)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getById_unknown_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id)).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void delete_unknown_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void search_delegatesToRepository() {
        PageResult<Product> expected = new PageResult<>(List.of(), 0, 20, 0, 0);
        when(productRepository.search(any())).thenReturn(expected);

        var result = service.search(new ProductSearchQuery(
                "wid", categoryId, null, null, true, 0, 20, "name", "asc"));

        assertThat(result).isSameAs(expected);
    }
}
