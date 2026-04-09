package com.checkstock.controller;

import com.checkstock.dto.SizeOptionDto;
import com.checkstock.dto.SnapshotResponse;
import com.checkstock.entity.Product;
import com.checkstock.repository.ProductRepository;
import com.checkstock.repository.SizeSnapshotRepository;
import com.checkstock.service.CrawlerService;
import com.checkstock.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}")
@RequiredArgsConstructor
public class SnapshotController {

    private final SizeSnapshotRepository snapshotRepository;
    private final ProductRepository productRepository;
    private final CrawlerService crawlerService;
    private final MonitoringService monitoringService;

    @GetMapping("/latest")
    public ResponseEntity<SnapshotResponse> getLatest(@PathVariable String productId) {
        return snapshotRepository.findTopByProductIdOrderByCheckedAtDesc(productId)
                .map(snapshot -> ResponseEntity.ok(SnapshotResponse.from(snapshot)))
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/check")
    public ResponseEntity<SnapshotResponse> triggerCheck(@PathVariable String productId) {
        return productRepository.findById(productId)
                .map(product -> {
                    List<SizeOptionDto> sizes = crawlerService.crawl(product);
                    monitoringService.process(product, sizes);
                    return snapshotRepository.findTopByProductIdOrderByCheckedAtDesc(productId)
                            .map(snapshot -> ResponseEntity.ok(SnapshotResponse.from(snapshot)))
                            .orElse(ResponseEntity.noContent().build());
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
