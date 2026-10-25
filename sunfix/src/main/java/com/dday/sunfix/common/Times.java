package com.dday.sunfix.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** 저장 시각은 전부 UTC ISO-8601 초 단위. */
public final class Times {
    private Times() {}
    public static String now() { return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(); }
    public static String after(long seconds) { return Instant.now().plusSeconds(seconds).truncatedTo(ChronoUnit.SECONDS).toString(); }
}
