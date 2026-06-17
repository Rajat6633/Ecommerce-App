package com.ecommerce.product.api.controller;

import com.ecommerce.product.api.dto.CategoryResponse;
import com.ecommerce.product.api.dto.CreateCategoryRequest;
import com.ecommerce.product.application.port.in.CategoryUseCase;
import com.ecommerce.product.application.port.in.CategoryUseCase.CreateCategoryCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Category management (admin) and public reads")
public class CategoryController {

    private final CategoryUseCase categories;

    public CategoryController(CategoryUseCase categories) {
        this.categories = categories;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a category (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest req) {
        var category = categories.create(new CreateCategoryCommand(req.name(), req.parentId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(category));
    }

    @GetMapping
    @Operation(summary = "List all categories")
    public List<CategoryResponse> findAll() {
        return categories.findAll().stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by id")
    public ResponseEntity<CategoryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(CategoryResponse.from(categories.getById(id)));
    }
}
