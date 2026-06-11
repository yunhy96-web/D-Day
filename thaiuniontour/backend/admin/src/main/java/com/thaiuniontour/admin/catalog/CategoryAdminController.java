package com.thaiuniontour.admin.catalog;

import com.thaiuniontour.admin.catalog.dto.CategoryFormDto;
import com.thaiuniontour.domain.catalog.model.Category;
import com.thaiuniontour.domain.catalog.model.CategoryType;
import com.thaiuniontour.domain.catalog.repository.CategoryRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/categories")
public class CategoryAdminController {

    private final CategoryRepository categoryRepository;

    public CategoryAdminController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String list(Model model) {
        List<Category> themes = categoryRepository.findByTypeAndUseYnTrueOrderBySortAsc(CategoryType.THEME);
        List<Category> destinations = categoryRepository.findByTypeAndUseYnTrueOrderBySortAsc(CategoryType.DESTINATION);

        List<Category> all = new ArrayList<>();
        all.addAll(themes);
        all.addAll(destinations);
        model.addAttribute("categories", all);
        return "catalog/category-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new CategoryFormDto());
        populateFormModel(model);
        return "catalog/category-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CategoryFormDto form,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            populateFormModel(model);
            return "catalog/category-form";
        }

        int level = 1;
        if (form.getParentId() != null) {
            level = categoryRepository.findById(form.getParentId())
                    .map(p -> p.getLevel() + 1)
                    .orElse(1);
        }

        Category category = Category.builder()
                .parentId(form.getParentId())
                .type(form.getType())
                .level(level)
                .name(form.getName())
                .slug(form.getSlug())
                .sort(form.getSort())
                .build();
        categoryRepository.save(category);

        return "redirect:/admin/categories";
    }

    private void populateFormModel(Model model) {
        model.addAttribute("categoryTypes", CategoryType.values());
        List<Category> themes = categoryRepository.findByTypeAndUseYnTrueOrderBySortAsc(CategoryType.THEME);
        List<Category> destinations = categoryRepository.findByTypeAndUseYnTrueOrderBySortAsc(CategoryType.DESTINATION);
        List<Category> allParents = new ArrayList<>();
        allParents.addAll(themes);
        allParents.addAll(destinations);
        model.addAttribute("parentCandidates", allParents);
    }
}
