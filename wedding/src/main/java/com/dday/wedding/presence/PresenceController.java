package com.dday.wedding.presence;

import com.dday.wedding.common.Valid;
import com.dday.wedding.security.ClientIp;
import com.dday.wedding.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 설계 §6. 15초 heartbeat, 40초 안 본 sid 는 제외. IP 당 최대 3명 집계(조작 방지 §13-2). */
@RestController
@RequiredArgsConstructor
public class PresenceController {
    private record Seen(Instant at, String ip) {}
    private final Map<String, Seen> seen = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbc;
    private final RateLimiter limiter;

    @PostMapping("/api/heartbeat")
    public Map<String, Object> heartbeat(@RequestBody Map<String, String> body, HttpServletRequest req) {
        limiter.check(req, "hb", 3, 10);
        String sid = Valid.sid(body.get("sid"));
        Instant now = Instant.now();
        seen.put(sid, new Seen(now, ClientIp.of(req)));
        seen.values().removeIf(s -> s.at.isBefore(now.minusSeconds(40)));
        jdbc.update("INSERT OR IGNORE INTO visit(session_id) VALUES (?)", sid);

        Map<String, Integer> perIp = new HashMap<>();
        seen.values().forEach(s -> perIp.merge(s.ip, 1, Integer::sum));
        int online = perIp.values().stream().mapToInt(n -> Math.min(n, 3)).sum();
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM visit", Integer.class);
        return Map.of("online", online, "total", total);
    }
}
