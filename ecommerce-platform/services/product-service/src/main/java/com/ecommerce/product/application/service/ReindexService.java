package com.ecommerce.product.application.service;

import com.ecommerce.product.application.port.in.ReindexUseCase;
import com.ecommerce.product.application.port.out.ProductIndexPort;
import com.ecommerce.product.application.port.out.ProductRepositoryPort;
import com.ecommerce.product.domain.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Rebuilds the product vector index from the catalog. Per-product fail-soft: the
 * underlying {@link ProductIndexPort#index} is Resilience4j-wrapped and swallows
 * failures, so one bad embed never aborts the whole rebuild.
 */
@Service
public class ReindexService implements ReindexUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReindexService.class);

    private final ProductRepositoryPort products;
    private final ProductIndexPort index;

    public ReindexService(ProductRepositoryPort products, ProductIndexPort index) {
        this.products = products;
        this.index = index;
    }

    @Override
    @Transactional(readOnly = true)
    public int reindexAll() {
        List<Product> all = products.findAll();
        for (Product product : all) {
            index.index(product);
        }
        log.info("Reindexed {} products into the vector store", all.size());
        return all.size();
    }
}
