package com.dday.wedding.label;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/** label 테이블 → {lang → {key → text}}. 60초 캐시(sqlite3 로 고치면 곧 반영). */
@Service
@RequiredArgsConstructor
public class LabelService {
    private final JdbcTemplate jdbc;
    private volatile Map<String, Map<String, String>> cache = Map.of();
    private volatile long loadedAt;

    public Map<String, Map<String, String>> all() {
        if (System.currentTimeMillis() - loadedAt > 60_000) {
            Map<String, Map<String, String>> m = new HashMap<>();
            jdbc.query("SELECT key, lang, text FROM label", (RowCallbackHandler) rs ->
                    m.computeIfAbsent(rs.getString("lang"), k -> new HashMap<>()).put(rs.getString("key"), rs.getString("text")));
            cache = m; loadedAt = System.currentTimeMillis();
        }
        return cache;
    }

    /** 해당 언어 맵 (없는 키는 ko fallback). 템플릿에서 t['key'] 로 사용 */
    public Map<String, String> forLang(String lang) {
        Map<String, String> ko = all().getOrDefault("ko", Map.of());
        Map<String, String> m = new HashMap<>(ko);
        m.putAll(all().getOrDefault(lang, Map.of()));
        return m;
    }
}
