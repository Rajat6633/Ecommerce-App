package com.ecommerce.cart.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign client for product-service. The URL is the Kubernetes Service DNS name
 * (no Eureka/Ribbon) — kube-proxy load-balances across pods.
 */
@FeignClient(name = "product-service", url = "${product.service.url}",
        configuration = FeignClientConfig.class)
public interface ProductFeignClient {

    @GetMapping("/api/products/{id}")
    ProductDto getById(@PathVariable("id") UUID id);
}
