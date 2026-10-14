package com.dday.family.auth;

import java.util.List;

/** 사람·교대 색 팔레트 키 (app.css 의 .c-{key}). Valid.COLORS 와 같은 목록 */
public final class Colors {
    private Colors() {}
    public static final List<String> ALL = List.of("blue", "green", "orange", "purple", "red", "sky", "amber", "gray");
    public static String forIndex(int i) { return ALL.get(Math.floorMod(i, ALL.size())); }
}
