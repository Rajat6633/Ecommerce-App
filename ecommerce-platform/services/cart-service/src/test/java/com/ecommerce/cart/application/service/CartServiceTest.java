package com.ecommerce.cart.application.service;

import com.ecommerce.cart.application.port.out.CartRepositoryPort;
import com.ecommerce.cart.application.port.out.ProductCatalogPort;
import com.ecommerce.cart.application.port.out.ProductCatalogPort.ProductInfo;
import com.ecommerce.cart.domain.exception.CartItemNotFoundException;
import com.ecommerce.cart.domain.exception.ProductNotFoundException;
import com.ecommerce.cart.domain.exception.ProductServiceUnavailableException;
import com.ecommerce.cart.domain.exception.ProductUnavailableException;
import com.ecommerce.cart.domain.model.Cart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class CartServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    @Mock CartRepositoryPort cartRepository;
    @Mock ProductCatalogPort productCatalog;

    private CartService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CartService(cartRepository, productCatalog, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ProductInfo activeProduct() {
        return new ProductInfo(productId, new BigDecimal("9.99"), "USD", true);
    }

    private Cart cartWithItem(int qty) {
        return Cart.empty(UUID.randomUUID(), userId, NOW)
                .addItem(productId, qty, new BigDecimal("9.99"), NOW);
    }

    @Test
    void addItem_validProduct_addsAndSaves() {
        when(productCatalog.findProduct(productId)).thenReturn(Optional.of(activeProduct()));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Cart result = service.addItem(userId, productId, 2);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
        verify(cartRepository).save(any());
    }

    @Test
    void addItem_unknownProduct_throwsNotFound() {
        when(productCatalog.findProduct(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addItem(userId, productId, 1))
                .isInstanceOf(ProductNotFoundException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItem_inactiveProduct_throwsUnavailable() {
        when(productCatalog.findProduct(productId))
                .thenReturn(Optional.of(new ProductInfo(productId, new BigDecimal("1.00"), "USD", false)));

        assertThatThrownBy(() -> service.addItem(userId, productId, 1))
                .isInstanceOf(ProductUnavailableException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItem_downstreamUnavailable_propagates503() {
        when(productCatalog.findProduct(productId)).thenThrow(new ProductServiceUnavailableException());

        assertThatThrownBy(() -> service.addItem(userId, productId, 1))
                .isInstanceOf(ProductServiceUnavailableException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void addItem_existingProduct_incrementsQuantity() {
        when(productCatalog.findProduct(productId)).thenReturn(Optional.of(activeProduct()));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cartWithItem(2)));
        when(cartRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Cart result = service.addItem(userId, productId, 3);

        assertThat(result.items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void updateItem_setsQuantity() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cartWithItem(2)));
        when(cartRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Cart result = service.updateItem(userId, productId, 7);

        assertThat(result.items().get(0).quantity()).isEqualTo(7);
    }

    @Test
    void updateItem_zeroQuantity_removesLine() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cartWithItem(2)));
        when(cartRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Cart result = service.updateItem(userId, productId, 0);

        assertThat(result.items()).isEmpty();
    }

    @Test
    void updateItem_missingLine_throwsNotFound() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(Cart.empty(UUID.randomUUID(), userId, NOW)));

        assertThatThrownBy(() -> service.updateItem(userId, productId, 3))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void removeItem_removesLine() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cartWithItem(2)));
        when(cartRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.removeItem(userId, productId).items()).isEmpty();
    }

    @Test
    void clearCart_emptiesItems() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cartWithItem(2)));
        when(cartRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.clearCart(userId).items()).isEmpty();
    }

    @Test
    void getCart_noExistingCart_returnsEmpty() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Cart result = service.getCart(userId);

        assertThat(result.items()).isEmpty();
        assertThat(result.userId()).isEqualTo(userId);
    }
}
