package com.ecommerce.product.api.controller;

import com.ecommerce.product.api.dto.CreateProductRequest;
import com.ecommerce.product.api.dto.PageResponse;
import com.ecommerce.product.api.dto.ProductResponse;
import com.ecommerce.product.api.dto.ScoredProductResponse;
import com.ecommerce.product.api.dto.UpdateProductRequest;
import com.ecommerce.product.application.port.in.ProductSearchQuery;
import com.ecommerce.product.application.port.in.ProductUseCase;
import com.ecommerce.product.application.port.in.ProductUseCase.CreateProductCommand;
import com.ecommerce.product.application.port.in.ProductUseCase.UpdateProductCommand;
import com.ecommerce.product.application.port.in.ReindexUseCase;
import com.ecommerce.product.application.port.in.SemanticSearchUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Catalog CRUD (admin) and public search")
public class ProductController {

    private final ProductUseCase products;
    private final SemanticSearchUseCase semanticSearch;
    private final ReindexUseCase reindex;

    public ProductController(ProductUseCase products,
                             SemanticSearchUseCase semanticSearch,
                             ReindexUseCase reindex) {
        this.products = products;
        this.semanticSearch = semanticSearch;
        this.reindex = reindex;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a product (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest req) {
        var product = products.create(new CreateProductCommand(
                req.sku(), req.name(), req.description(), req.price(), req.currency(), req.categoryId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a product (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateProductRequest req) {
        var product = products.update(id, new UpdateProductCommand(
                req.name(), req.description(), req.price(), req.currency(), req.categoryId(), req.active()));
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a product (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        products.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ProductResponse.from(products.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Search products (filters + pagination + sorting)")
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        var result = products.search(new ProductSearchQuery(
                name, categoryId, minPrice, maxPrice, activeOnly, page, size, sortBy, sortDirection));
        return PageResponse.from(result, ProductResponse::from);
    }

    // ---------------------------------------------------------------------
    //  D1 — Semantic product search (embedding-based, public)
    // ---------------------------------------------------------------------

    @GetMapping("/search")
    @Operation(summary = "Semantic product search (D1): embed the query, return nearest products with scores")
    public PageResponse<ScoredProductResponse> semanticSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = semanticSearch.semanticSearch(q, page, size);
        return PageResponse.from(result, ScoredProductResponse::from);
    }

    // ---------------------------------------------------------------------
    //  D5 — Recommendations (content-based, public)
    // ---------------------------------------------------------------------

    @GetMapping("/{id}/recommendations")
    @Operation(summary = "Recommend similar products (D5): top-N by vector similarity, excluding the product itself")
    public List<ScoredProductResponse> recommendations(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "10") int limit) {
        return semanticSearch.recommendations(id, limit).stream()
                .map(ScoredProductResponse::from)
                .toList();
    }

    // ---------------------------------------------------------------------
    //  Admin — one-shot rebuild of the vector index
    // ---------------------------------------------------------------------

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rebuild the product vector index (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ReindexResponse> reindex() {
        return ResponseEntity.ok(new ReindexResponse(reindex.reindexAll()));
    }

    /** Result of a reindex run. */
    public record ReindexResponse(int reindexed) {}
}
