package com.dday.sunfix.common;

import java.time.YearMonth;
import java.util.Set;
import java.util.regex.Pattern;

/** 화이트리스트 검증 헬퍼. 통과 못 하면 BadRequest(code). */
public final class Valid {
    private Valid() {}
    private static final Pattern CTRL = Pattern.compile("[\\p{Cntrl}&&[^\\n]]");
    private static final Pattern PHONE = Pattern.compile("^[0-9+\\- ]{8,20}$");
    private static final Pattern YM = Pattern.compile("^20\\d{2}-(0[1-9]|1[0-2])$");
    private static final Pattern CODE = Pattern.compile("^[A-Z0-9_]{1,40}$");
    public static final Set<String> LANGS = Set.of("ko", "en");
    public static final Set<String> THEMES = Set.of("system", "light", "dark");
    public static final int MAX_PRICE = 1_000_000_000;

    public static String text(String s, int max, String code) {
        String t = optText(s, max, code);
        if (t == null) throw new BadRequest(code);
        return t;
    }
    /** 제어문자 제거 + 앞뒤 공백 제거. 비어 있으면 null, max 초과면 BadRequest */
    public static String optText(String s, int max, String code) {
        if (s == null) return null;
        s = CTRL.matcher(s).replaceAll("").strip();
        if (s.isEmpty()) return null;
        if (s.codePointCount(0, s.length()) > max) throw new BadRequest(code);
        return s;
    }
    public static String phone(String s) {
        if (s == null || !PHONE.matcher(s.strip()).matches()) throw new BadRequest("phone");
        return s.strip();
    }
    public static String ym(String s) {
        // 미래 달은 안 됨
        if (s == null || !YM.matcher(s).matches() || s.compareTo(YearMonth.now().toString()) > 0) throw new BadRequest("ym");
        return s;
    }
    public static String code(String s) {
        if (s == null || !CODE.matcher(s).matches()) throw new BadRequest("code");
        return s;
    }
    /** null 허용(미공개). 0 ~ 10억 */
    public static Integer optPrice(Integer p) {
        if (p != null && (p < 0 || p > MAX_PRICE)) throw new BadRequest("price");
        return p;
    }
    public static int price(Integer p) {
        if (p == null || p <= 0 || p > MAX_PRICE) throw new BadRequest("price");
        return p;
    }
    public static String oneOf(String s, Set<String> allowed, String code) {
        if (s == null || !allowed.contains(s)) throw new BadRequest(code);
        return s;
    }
}
