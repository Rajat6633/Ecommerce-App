package com.ecommerce.order.application.port.in;

import com.ecommerce.order.domain.model.Order;

import java.util.UUID;

/** Command side: place a new order from the user's cart (saga start). */
public interface PlaceOrderUseCase {
    Order placeOrder(UUID userId);
}
