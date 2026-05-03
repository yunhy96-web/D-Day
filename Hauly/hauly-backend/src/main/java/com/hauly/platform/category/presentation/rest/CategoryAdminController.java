package com.hauly.platform.category.presentation.rest;

import com.hauly.platform.category.application.CategoryService;
import com.hauly.platform.category.application.command.CreateCategoryCommand;
import com.hauly.platform.category.application.query.CategoryDetailView;
import com.hauly.platform.category.application.query.CategoryView;
import com.hauly.platform.category.presentation.dto.CreateCategoryRequest;
import com.hauly.platform.category.presentation.dto.UpdateSchemaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoint for managing product categories.
 * Requires ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class CategoryAdminController {

    private final CategoryService categoryService;

    public CategoryAdminController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryView> createCategory(@Valid @RequestBody CreateCategoryRequest req) {
        CreateCategoryCommand command = new CreateCategoryCommand(
                req.code(), req.nameKey(), req.fieldSchema(), req.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(command));
    }

    @PatchMapping("/{code}")
    public ResponseEntity<CategoryDetailView> updateSchema(
            @PathVariable String code,
            @Valid @RequestBody UpdateSchemaRequest req) {
        return ResponseEntity.ok(categoryService.updateSchema(code, req.fieldSchema()));
    }
}
