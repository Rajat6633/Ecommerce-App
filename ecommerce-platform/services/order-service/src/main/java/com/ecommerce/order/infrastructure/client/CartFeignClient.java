package com.ecommerce.order.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "cart-service", url = "${cart.service.url}",
        configuration = FeignClientConfig.class)
public interface CartFeignClient {

    @GetMapping("/api/cart")
    CartDto getCart();

    @DeleteMapping("/api/cart")
    void clearCart();
}
