package com.checkstock.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String sizeSelector = "li.variations-attribute";

    @Column(nullable = false)
    private String soldOutIndicator = "out";

    private String targetSize; // 클릭할 허리 사이즈 (예: "30,31,32"). null이면 클릭 없이 그대로 조회

    private String alertSizeFilter; // 알림 받을 사이즈 필터 (콤마 구분, 예: "L,XL"). null이면 전체

    private int refreshInterval = 300;

    private boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
