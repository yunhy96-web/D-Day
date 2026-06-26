package com.thaiuniontour.admin.catalog;

import com.thaiuniontour.admin.catalog.dto.ScheduleFormDto;
import com.thaiuniontour.domain.catalog.model.Product;
import com.thaiuniontour.domain.catalog.model.ProductSchedule;
import com.thaiuniontour.domain.catalog.model.ScheduleStatus;
import com.thaiuniontour.domain.catalog.repository.ProductRepository;
import com.thaiuniontour.domain.catalog.repository.ProductScheduleRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/admin/products/{productId}/schedules")
public class ProductScheduleAdminController {

    private final ProductRepository productRepository;
    private final ProductScheduleRepository productScheduleRepository;

    public ProductScheduleAdminController(ProductRepository productRepository,
                                          ProductScheduleRepository productScheduleRepository) {
        this.productRepository = productRepository;
        this.productScheduleRepository = productScheduleRepository;
    }

    @GetMapping
    public String list(@PathVariable UUID productId, Model model) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        model.addAttribute("product", product);
        model.addAttribute("schedules", productScheduleRepository.findByProductIdOrderBySaleDateAsc(productId));
        model.addAttribute("form", new ScheduleFormDto());
        model.addAttribute("scheduleStatuses", ScheduleStatus.values());
        return "catalog/schedule-list";
    }

    @PostMapping
    public String create(@PathVariable UUID productId,
                         @ModelAttribute("form") ScheduleFormDto form) {
        ProductSchedule schedule = ProductSchedule.builder()
                .productId(productId)
                .saleDate(form.getSaleDate())
                .stock(form.getStock())
                .price(form.getPrice())
                .status(form.getStatus())
                .build();
        productScheduleRepository.save(schedule);
        return "redirect:/admin/products/" + productId + "/schedules";
    }
}
