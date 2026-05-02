package com.checkstockbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "site_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class SiteSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String siteId;

    /** 전체 상품 개수 (필터 전) */
    private int totalCount;

    /** 필터 통과 상품 개수 */
    private int matchedCount;

    /** 필터 통과 상품 JSON ({name,url}[]) */
    @Column(columnDefinition = "TEXT")
    private String matchedProducts;

    @Column(nullable = false)
    private LocalDateTime checkedAt;
}
