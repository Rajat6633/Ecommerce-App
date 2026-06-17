package com.ecommerce.cart.application.service;

import com.ecommerce.cart.application.port.in.CartUseCase;
import com.ecommerce.cart.application.port.out.CartRepositoryPort;
import com.ecommerce.cart.application.port.out.ProductCatalogPort;
import com.ecommerce.cart.application.port.out.ProductCatalogPort.ProductInfo;
import com.ecommerce.cart.domain.exception.ProductNotFoundException;
import com.ecommerce.cart.domain.exception.ProductUnavailableException;
import com.ecommerce.cart.domain.model.Cart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class CartService implements CartUseCase {

    private final CartRepositoryPort cartRepository;
    private final ProductCatalogPort productCatalog;
    private final Clock clock;

    public CartService(CartRepositoryPort cartRepository, ProductCatalogPort productCatalog, Clock clock) {
        this.cartRepository = cartRepository;
        this.productCatalog = productCatalog;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Cart getCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.empty(UUID.randomUUID(), userId, clock.instant()));
    }

    @Override
    @Transactional
    public Cart addItem(UUID userId, UUID productId, int quantity) {
        ProductInfo product = productCatalog.findProduct(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId.toString()));
        if (!product.active()) {
            throw new ProductUnavailableException(productId.toString());
        }
        Cart cart = getOrCreate(userId)
                .addItem(productId, quantity, product.price(), clock.instant());
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public Cart updateItem(UUID userId, UUID productId, int quantity) {
        Cart cart = getOrCreate(userId).updateItem(productId, quantity, clock.instant());
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public Cart removeItem(UUID userId, UUID productId) {
        Cart cart = getOrCreate(userId).removeItem(productId, clock.instant());
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public Cart clearCart(UUID userId) {
        Cart cart = getOrCreate(userId).clear(clock.instant());
        return cartRepository.save(cart);
    }

    private Cart getOrCreate(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> Cart.empty(UUID.randomUUID(), userId, clock.instant()));
    }
}
