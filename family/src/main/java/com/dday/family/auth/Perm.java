package com.dday.family.auth;

/** 권한 코드(설계 §4-1). 새 기능은 여기 상수 + role_permission 행 추가. ADMIN 은 '*'. */
public final class Perm {
    private Perm() {}
    public static final String POST_WRITE = "post.write";
    public static final String POST_DELETE_ANY = "post.delete.any";
    public static final String COMMENT_WRITE = "comment.write";
    public static final String COMMENT_DELETE_ANY = "comment.delete.any";
    public static final String MEDIA_UPLOAD = "media.upload";
    public static final String MEDIA_ORIGINAL = "media.original.download";
    public static final String SHIFT_WRITE = "shift.write";
    public static final String SHIFT_WRITE_ANY = "shift.write.any";
    public static final String MEMBER_APPROVE = "member.approve";
    public static final String MEMBER_MANAGE = "member.manage";
    public static final String ROLE_MANAGE = "role.manage";
}
