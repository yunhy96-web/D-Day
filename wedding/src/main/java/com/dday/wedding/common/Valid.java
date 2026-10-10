package com.dday.wedding.common;

import java.util.regex.Pattern;

/** 화이트리스트 검증 헬퍼. 통과 못 하면 BadRequest. */
public final class Valid {
    private Valid() {}
    private static final Pattern SID = Pattern.compile("^[0-9a-fA-F-]{36}$");
    private static final Pattern CTRL = Pattern.compile("[\\p{Cntrl}&&[^\\n]]");

    public static String sid(String s) {
        if (s == null || !SID.matcher(s).matches()) throw new BadRequest("sid");
        return s.toLowerCase();
    }
    public static String text(String s, int max, String code) {
        if (s == null) throw new BadRequest(code);
        s = CTRL.matcher(s).replaceAll("").strip();
        if (s.isEmpty() || s.length() > max) throw new BadRequest(code);
        return s;
    }
    public static String lang(String l) {
        return l != null && (l.equals("ko") || l.equals("en") || l.equals("th")) ? l : "ko";
    }
}
