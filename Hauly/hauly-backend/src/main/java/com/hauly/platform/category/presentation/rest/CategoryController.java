package com.hauly.platform.category.presentation.rest;

import com.hauly.platform.category.application.CategoryService;
import com.hauly.platform.category.application.query.CategoryDetailView;
import com.hauly.platform.category.application.query.CategoryView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Authenticated intake endpoint for reading categories.
 */
@RestController
@RequestMapping("/api/intake/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryView>> getActiveCategories() {
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }

    @GetMapping("/{code}")
    public ResponseEntity<CategoryDetailView> getCategory(@PathVariable String code) {
        return ResponseEntity.ok(categoryService.getCategory(code));
    }
}
