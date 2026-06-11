package com.thaiuniontour.admin.catalog;

import com.thaiuniontour.admin.catalog.dto.ProductFormDto;
import com.thaiuniontour.domain.catalog.model.Category;
import com.thaiuniontour.domain.catalog.model.CategoryType;
import com.thaiuniontour.domain.catalog.model.Product;
import com.thaiuniontour.domain.catalog.model.ProductCategory;
import com.thaiuniontour.domain.catalog.model.ProductStatus;
import com.thaiuniontour.domain.catalog.model.StockType;
import com.thaiuniontour.domain.catalog.repository.CategoryRepository;
import com.thaiuniontour.domain.catalog.repository.ProductCategoryRepository;
import com.thaiuniontour.domain.catalog.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/products")
public class ProductAdminController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final AdminProductService adminProductService;

    public ProductAdminController(ProductRepository productRepository,
                                  CategoryRepository categoryRepository,
                                  ProductCategoryRepository productCategoryRepository,
                                  AdminProductService adminProductService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.adminProductService = adminProductService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "catalog/product-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new ProductFormDto());
        populateFormModel(model);
        return "catalog/product-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProductFormDto form,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            populateFormModel(model);
            return "catalog/product-form";
        }
        adminProductService.create(form);
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + id));

        ProductFormDto form = new ProductFormDto();
        form.setName(product.getName());
        form.setSummary(product.getSummary());
        form.setDescription(product.getDescription());
        form.setStockType(product.getStockType());
        form.setBasePrice(product.getBasePrice());
        form.setStatus(product.getStatus());
        form.setThumbnailUrl(product.getThumbnailUrl());

        List<ProductCategory> pcs = productCategoryRepository.findByIdProductId(id);
        for (ProductCategory pc : pcs) {
            categoryRepository.findById(pc.getCategoryId()).ifPresent(cat -> {
                if (cat.getType() == CategoryType.THEME) {
                    form.setThemeCategoryId(cat.getId());
                } else if (cat.getType() == CategoryType.DESTINATION) {
                    form.setDestinationCategoryId(cat.getId());
                }
            });
        }

        model.addAttribute("form", form);
        model.addAttribute("productId", id);
        populateFormModel(model);
        return "catalog/product-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id,
                         @Valid @ModelAttribute("form") ProductFormDto form,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("productId", id);
            populateFormModel(model);
            return "catalog/product-form";
        }
        adminProductService.update(id, form);
        return "redirect:/admin/products";
    }

    private void populateFormModel(Model model) {
        model.addAttribute("stockTypes", StockType.values());
        model.addAttribute("productStatuses", ProductStatus.values());
        model.addAttribute("themeCategories",
                categoryRepository.findByTypeAndUseYnTrueOrderBySortAsc(CategoryType.THEME));
        model.addAttribute("destinationCategories",
                categoryRepository.findByTypeAndUseYnTrueOrderBySortAsc(CategoryType.DESTINATION));
    }
}
