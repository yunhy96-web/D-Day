package com.dday.family.common;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;

/** 화이트리스트 검증 헬퍼. 통과 못 하면 BadRequest(code). */
public final class Valid {
    private Valid() {}
    private static final Pattern CTRL = Pattern.compile("[\\p{Cntrl}&&[^\\n]]");
    private static final Pattern LOGIN_ID = Pattern.compile("^[a-z0-9]{4,20}$");
    private static final Pattern HHMM = Pattern.compile("^\\d{2}:\\d{2}$");
    public static final Set<String> LANGS = Set.of("ko", "th");
    public static final Set<String> THEMES = Set.of("system", "light", "dark");
    public static final Set<String> FONTS = Set.of("sm", "md", "lg");
    public static final Set<String> COLORS = Set.of("blue", "sky", "green", "amber", "orange", "red", "purple", "gray");
    public static final Set<String> TIMEZONES = Set.of("Asia/Seoul", "Asia/Bangkok");

    /** 제어문자 제거 + 앞뒤 공백 제거. 빈 값이나 max 초과면 BadRequest */
    public static String text(String s, int max, String code) {
        String t = optText(s, max, code);
        if (t == null) throw new BadRequest(code);
        return t;
    }
    /** 비어 있으면 null */
    public static String optText(String s, int max, String code) {
        if (s == null) return null;
        s = CTRL.matcher(s).replaceAll("").strip();
        if (s.isEmpty()) return null;
        if (s.codePointCount(0, s.length()) > max) throw new BadRequest(code);
        return s;
    }
    public static String loginId(String s) {
        String v = s == null ? "" : s.strip().toLowerCase();
        if (!LOGIN_ID.matcher(v).matches()) throw new BadRequest("loginId");
        return v;
    }
    /** BCrypt 는 72바이트까지만 본다 */
    public static String password(String s) {
        if (s == null || s.length() < 8 || s.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) throw new BadRequest("password");
        return s;
    }
    public static String oneOf(String s, Set<String> allowed, String code) {
        if (s == null || !allowed.contains(s)) throw new BadRequest(code);
        return s;
    }
    public static String date(String s) {
        try { return LocalDate.parse(s).toString(); } catch (DateTimeParseException | NullPointerException e) { throw new BadRequest("date"); }
    }
    /** 'HH:mm' 또는 null */
    public static String optTime(String s) {
        if (s == null || s.isBlank()) return null;
        if (!HHMM.matcher(s).matches()) throw new BadRequest("time");
        try { LocalTime.parse(s); } catch (DateTimeParseException e) { throw new BadRequest("time"); }
        return s;
    }
}
