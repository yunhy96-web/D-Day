package com.thaiuniontour.admin.catalog.dto;

import com.thaiuniontour.domain.catalog.model.ProductStatus;
import com.thaiuniontour.domain.catalog.model.StockType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class ProductFormDto {

    @NotBlank
    private String name;

    private String summary;

    private String description;

    @NotNull
    private StockType stockType;

    @Min(0)
    private long basePrice;

    @NotNull
    private ProductStatus status;

    private String thumbnailUrl;

    @NotNull
    private UUID themeCategoryId;

    private UUID destinationCategoryId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public StockType getStockType() { return stockType; }
    public void setStockType(StockType stockType) { this.stockType = stockType; }

    public long getBasePrice() { return basePrice; }
    public void setBasePrice(long basePrice) { this.basePrice = basePrice; }

    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public UUID getThemeCategoryId() { return themeCategoryId; }
    public void setThemeCategoryId(UUID themeCategoryId) { this.themeCategoryId = themeCategoryId; }

    public UUID getDestinationCategoryId() { return destinationCategoryId; }
    public void setDestinationCategoryId(UUID destinationCategoryId) { this.destinationCategoryId = destinationCategoryId; }
}
