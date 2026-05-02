package com.checkstockbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "monitored_sites")
@Getter
@Setter
@NoArgsConstructor
public class MonitoredSite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private String baseUrl;

    /** 상품 링크 CSS 셀렉터 (예: a.name-link.js-pdp-link) */
    @Column(nullable = false)
    private String listSelector;

    private int refreshIntervalSec = 300;

    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "site_include_keywords", joinColumns = @JoinColumn(name = "site_id"))
    @Column(name = "keyword", length = 200)
    private List<String> includeKeywords = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "site_exclude_keywords", joinColumns = @JoinColumn(name = "site_id"))
    @Column(name = "keyword", length = 200)
    private List<String> excludeKeywords = new ArrayList<>();

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
