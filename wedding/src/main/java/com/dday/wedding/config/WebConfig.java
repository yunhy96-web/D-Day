package com.dday.wedding.config;

import com.dday.wedding.admin.AdminInterceptor;
import com.dday.wedding.security.OriginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final OriginInterceptor originInterceptor;
    private final AdminInterceptor adminInterceptor;
    @Value("${app.data-dir}") private String dataDir;

    /** 사진은 보통 Ncloud 서명 URL 로 나간다(설계 §15-10). 키가 없을 때만 로컬 서빙. 파일명이 UUID 라 1년 immutable */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 로컬 모드(Ncloud 키 없음)에서만 쓰인다. data/photos/original 은 매핑하지 않음 → 원본 비공개
        serve(registry, "/files/photos/view/**", "photos/view");
        serve(registry, "/files/photos/thumb/**", "photos/thumb");
        serve(registry, "/files/timeline/**", "timeline");
        // 하객 사진은 어드민에서 지우면 빨리 빠지도록 1시간
        serve(registry, "/files/guestbook/**", "guestbook", CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());
    }

    private void serve(ResourceHandlerRegistry registry, String pattern, String dir) {
        serve(registry, pattern, dir, CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
    }

    private void serve(ResourceHandlerRegistry registry, String pattern, String dir, CacheControl cache) {
        registry.addResourceHandler(pattern)
                .addResourceLocations("file:" + Path.of(dataDir, dir).toAbsolutePath() + "/")   // 디렉토리가 아직 없어도 동작
                .setCacheControl(cache);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(originInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(adminInterceptor).addPathPatterns("/admin/api/**");
    }
}
