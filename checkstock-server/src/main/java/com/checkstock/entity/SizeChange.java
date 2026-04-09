package com.checkstock.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "size_changes", indexes = {
        @Index(name = "idx_size_changes_product", columnList = "productId, detectedAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class SizeChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String productId;

    @Column(columnDefinition = "TEXT")
    private String becameAvailable; // JSON: [{label, available, group}]

    @Column(columnDefinition = "TEXT")
    private String becameSoldOut; // JSON: [{label, available, group}]

    private int oldAvailableCount;
    private int newAvailableCount;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    public void prePersist() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }
}
