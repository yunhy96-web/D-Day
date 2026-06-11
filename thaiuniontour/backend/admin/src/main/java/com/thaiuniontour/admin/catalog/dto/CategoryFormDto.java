package com.thaiuniontour.admin.catalog.dto;

import com.thaiuniontour.domain.catalog.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CategoryFormDto {

    @NotNull
    private CategoryType type;

    private UUID parentId;

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private int sort;

    public CategoryType getType() { return type; }
    public void setType(CategoryType type) { this.type = type; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public int getSort() { return sort; }
    public void setSort(int sort) { this.sort = sort; }
}
