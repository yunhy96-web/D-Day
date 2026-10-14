package com.dday.family.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** 저장 시각은 전부 UTC ISO-8601 초 단위 (설계 §9). */
public final class Times {
    private Times() {}
    public static String now() { return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(); }
    public static String ago(long seconds) { return Instant.now().minusSeconds(seconds).truncatedTo(ChronoUnit.SECONDS).toString(); }
}
