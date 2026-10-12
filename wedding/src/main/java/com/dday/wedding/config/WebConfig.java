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

    /** 파일명이 전부 UUID 라 내용이 바뀌지 않음 → 기본 1년 immutable. data/photos/original 은 서빙하지 않는다(설계 §15-6) */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        serve(registry, "/photos/view/**", "photos/view");
        serve(registry, "/photos/thumb/**", "photos/thumb");
        serve(registry, "/timeline/**", "timeline");
        // 하객 사진은 어드민에서 지우면 Cloudflare 캐시에서도 빨리 빠지도록 1시간
        serve(registry, "/guestbook-photos/**", "guestbook", CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());
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
