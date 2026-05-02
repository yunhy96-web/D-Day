package com.checkstockbackend.dto;

import com.checkstockbackend.entity.MonitoredSite;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SiteResponse {
    private String id;
    private String name;
    private String url;
    private boolean active;
    private List<String> includeKeywords;
    private List<String> excludeKeywords;

    public static SiteResponse from(MonitoredSite s) {
        return new SiteResponse(
                s.getId(),
                s.getName(),
                s.getUrl(),
                s.isActive(),
                s.getIncludeKeywords(),
                s.getExcludeKeywords()
        );
    }
}
