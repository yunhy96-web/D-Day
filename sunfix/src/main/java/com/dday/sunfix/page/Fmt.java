package com.dday.sunfix.page;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/** 템플릿용 표시 헬퍼 ${f}. 금액은 KRW 고정, 환율 변환 없음(설계 §5). */
public record Fmt(String lang, Map<String, String> labels) {
    private static final NumberFormat NUM = NumberFormat.getIntegerInstance(Locale.US);

    /** null(미공개)이면 '문의 필요' */
    public String won(Integer n) {
        if (n == null) return labels.getOrDefault("price.hidden", "-");
        String s = NUM.format(n);
        return "en".equals(lang) ? "₩" + s : s + "원";
    }
    /** 공통코드 이름 */
    public String n(String code) { return code == null ? "" : labels.getOrDefault("code." + code, code); }
    /** 라벨 + {0}.. 치환 */
    public String t(String key, Object... args) {
        String s = labels.getOrDefault(key, key);
        for (int i = 0; i < args.length; i++) s = s.replace("{" + i + "}", String.valueOf(args[i]));
        return s;
    }
}
