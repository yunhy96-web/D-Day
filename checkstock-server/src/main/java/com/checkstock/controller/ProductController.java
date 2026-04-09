package com.checkstock.controller;

import com.checkstock.dto.ProductRequest;
import com.checkstock.entity.Product;
import com.checkstock.repository.ProductRepository;
import com.checkstock.repository.SizeChangeRepository;
import com.checkstock.repository.SizeSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final SizeSnapshotRepository snapshotRepository;
    private final SizeChangeRepository changeRepository;

    @GetMapping
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @PostMapping
    public Product create(@RequestBody ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setUrl(request.getUrl());
        product.setSizeSelector(request.getSizeSelector());
        product.setSoldOutIndicator(request.getSoldOutIndicator());
        product.setTargetSize(request.getTargetSize());
        product.setRefreshInterval(request.getRefreshInterval());
        return productRepository.save(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable String id, @RequestBody ProductRequest request) {
        return productRepository.findById(id)
                .map(product -> {
                    if (request.getName() != null) product.setName(request.getName());
                    if (request.getUrl() != null) product.setUrl(request.getUrl());
                    if (request.getSizeSelector() != null) product.setSizeSelector(request.getSizeSelector());
                    if (request.getSoldOutIndicator() != null) product.setSoldOutIndicator(request.getSoldOutIndicator());
                    product.setTargetSize(request.getTargetSize());
                    if (request.getRefreshInterval() > 0) product.setRefreshInterval(request.getRefreshInterval());
                    return ResponseEntity.ok(productRepository.save(product));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
