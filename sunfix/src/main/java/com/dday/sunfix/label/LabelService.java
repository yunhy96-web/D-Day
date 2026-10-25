package com.dday.sunfix.label;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** label 테이블 → {lang → {key → text}} (설계 §6). 60초 캐시, 관리자 저장 시 즉시 무효화. 없는 키는 ko fallback. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabelService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private volatile Map<String, Map<String, String>> cache = Map.of();
    private volatile long loadedAt;

    private Map<String, Map<String, String>> all() {
        if (System.currentTimeMillis() - loadedAt > 60_000) {
            Map<String, Map<String, String>> m = new HashMap<>();
            jdbc.query("SELECT key, lang, text FROM label", (RowCallbackHandler) rs ->
                    m.computeIfAbsent(rs.getString("lang"), k -> new HashMap<>()).put(rs.getString("key"), rs.getString("text")));
            cache = m; loadedAt = System.currentTimeMillis();
        }
        return cache;
    }

    public void invalidate() { loadedAt = 0; }

    /** fallback 없이 그 언어만 (관리자 편집용) */
    public Map<String, String> raw(String lang) { return all().getOrDefault(lang, Map.of()); }

    public Map<String, String> forLang(String lang) {
        Map<String, String> m = new HashMap<>(all().getOrDefault("ko", Map.of()));
        m.putAll(all().getOrDefault(lang, Map.of()));
        return m;
    }

    /** <script type="application/json"> 에 넣을 JSON. '<' 를 이스케이프해 </script> 로 못 빠져나가게 */
    public String json(String lang) {
        try { return json.writeValueAsString(forLang(lang)).replace("<", "\\u003c"); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warnMissing() {
        Set<String> ko = new HashSet<>(all().getOrDefault("ko", Map.of()).keySet());
        ko.removeAll(all().getOrDefault("en", Map.of()).keySet());
        if (!ko.isEmpty()) log.warn("영어 번역 없는 라벨 {}개: {}", ko.size(), ko);
    }
}
