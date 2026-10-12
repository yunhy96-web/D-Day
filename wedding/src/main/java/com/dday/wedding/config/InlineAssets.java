package com.dday.wedding.config;

import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 하객 페이지의 CSS·공통 JS 를 HTML 에 직접 넣는다 (설계 §15-12).
 * Cloudflare 무료 경로(시애틀)는 왕복 1번이 1~3초라, 파일 3개를 따로 받는 것보다 HTML 에 10KB 더 싣는 게 빠르다.
 * 우리 빌드 산출물만 넣으므로 th:utext 예외. 기동 시 1번 읽음.
 */
@Getter
@Component
public class InlineAssets {
    private final String css;
    private final String js;

    public InlineAssets() throws IOException {
        this.css = read("static/tw.css") + "\n" + read("static/app.css");
        this.js = read("static/app.js");
        if (css.contains("</style") || js.contains("</script")) throw new IllegalStateException("inline asset contains closing tag");
    }

    private static String read(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
