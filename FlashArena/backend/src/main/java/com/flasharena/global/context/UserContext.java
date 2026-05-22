package com.flasharena.global.context;

import java.util.UUID;

/**
 * 요청 스레드 단위로 인증된 사용자(UUID/role)를 보관하는 ThreadLocal.
 * AuthInterceptor 가 채우고 afterCompletion 에서 비운다.
 * order / payment 도메인은 여기서 현재 사용자 UUID 를 읽어 다운스트림으로 전파한다.
 */
public final class UserContext {

    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UUID userId, String role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    public static UUID getUserId() {
        return USER_ID.get();
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
