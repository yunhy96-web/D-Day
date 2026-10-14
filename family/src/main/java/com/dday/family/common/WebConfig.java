package com.dday.family.common;

import com.dday.family.auth.AuthInterceptor;
import com.dday.family.auth.MeArgumentResolver;
import com.dday.family.security.OriginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final OriginInterceptor originInterceptor;
    private final MeArgumentResolver meResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(originInterceptor).addPathPatterns("/api/**", "/admin/api/**");
        registry.addInterceptor(authInterceptor).addPathPatterns("/**").excludePathPatterns("/static/**", "/error", "/favicon.ico", "/robots.txt");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) { resolvers.add(meResolver); }
}
