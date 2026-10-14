package com.dday.family.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 고정 윈도우 카운터. 인스턴스 1개라 메모리로 충분. ponytail: 맵이 5000 넘으면 통째로 비움, 정밀도보다 단순함. */
@Component
public class RateLimiter {
    private record Window(long slot, AtomicInteger n) {}
    private final Map<String, Window> buckets = new ConcurrentHashMap<>();

    /** @return true 면 허용 */
    public boolean allow(String key, int limit, int windowSec) {
        long slot = System.currentTimeMillis() / 1000 / windowSec;
        if (buckets.size() > 5000) buckets.clear();
        Window w = buckets.compute(key, (k, old) -> old == null || old.slot != slot ? new Window(slot, new AtomicInteger()) : old);
        return w.n.incrementAndGet() <= limit;
    }

    public void check(HttpServletRequest req, String action, int limit, int windowSec) {
        if (!allow(action + ":" + ClientIp.of(req), limit, windowSec)) throw new TooManyRequests();
    }

    public static class TooManyRequests extends RuntimeException {}
}
