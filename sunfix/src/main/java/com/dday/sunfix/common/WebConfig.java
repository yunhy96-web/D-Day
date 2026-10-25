package com.dday.sunfix.common;

import com.dday.sunfix.admin.AdminAuth;
import com.dday.sunfix.security.OriginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final AdminAuth adminAuth;
    private final OriginInterceptor originInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(originInterceptor).addPathPatterns("/api/**", "/admin/api/**");
        registry.addInterceptor(adminAuth).addPathPatterns("/admin", "/admin/**");
    }
}
