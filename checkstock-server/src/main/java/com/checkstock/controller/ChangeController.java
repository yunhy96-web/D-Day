package com.checkstock.controller;

import com.checkstock.dto.ChangeResponse;
import com.checkstock.repository.SizeChangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/changes")
@RequiredArgsConstructor
public class ChangeController {

    private final SizeChangeRepository changeRepository;

    @GetMapping
    public List<ChangeResponse> getChanges(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return changeRepository.findByProductIdOrderByDetectedAtDesc(productId, PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(ChangeResponse::from)
                .toList();
    }
}
