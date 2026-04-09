package com.checkstock.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "size_snapshots", indexes = {
        @Index(name = "idx_size_snapshots_product", columnList = "productId, checkedAt DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class SizeSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String productId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String sizes; // JSON: [{label, available, group}]

    private int availableCount;
    private int totalCount;

    @Column(nullable = false)
    private LocalDateTime checkedAt;

    @PrePersist
    public void prePersist() {
        if (checkedAt == null) {
            checkedAt = LocalDateTime.now();
        }
    }
}
