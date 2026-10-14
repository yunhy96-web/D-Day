package com.dday.family.auth;

import com.dday.family.common.Forbidden;

import java.util.Set;

/** 로그인한 사람 + 지금 보고 있는 가족. 요청마다 AuthInterceptor 가 만든다. 컨트롤러는 파라미터로 받는다. */
public record Me(long memberId, long familyId, String loginId, String name, String color,
                 String lang, String theme, String fontScale, boolean fontScaleSet, String timezone,
                 String status, boolean mustChangePw, Set<String> perms) {

    public boolean has(String perm) { return perms.contains("*") || perms.contains(perm); }

    public void require(String perm) { if (!has(perm)) throw new Forbidden(); }

    public boolean active() { return "ACTIVE".equals(status); }

    public boolean admin() { return has("member.approve") || has("member.manage"); }
}
