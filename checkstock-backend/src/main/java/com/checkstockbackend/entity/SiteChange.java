package com.checkstockbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "site_changes")
@Getter
@Setter
@NoArgsConstructor
public class SiteChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String siteId;

    @Column(columnDefinition = "TEXT")
    private String addedProducts;

    @Column(columnDefinition = "TEXT")
    private String removedProducts;

    private int oldMatchedCount;

    private int newMatchedCount;

    @Column(nullable = false)
    private LocalDateTime detectedAt;
}
