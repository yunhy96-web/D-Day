package com.dday.wedding.content;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/** 컨텐츠 허브 (설계 §4-3). content 테이블 1행 = 카드 1장. */
@Service
@RequiredArgsConstructor
public class ContentService {
    private final JdbcTemplate jdbc;

    public record Card(String key, String path, String icon, Long stat) {}

    public List<Card> cards() {
        return jdbc.query("SELECT key, path, icon, stat_sql FROM content WHERE enabled_yn='Y' ORDER BY sort",
                (rs, i) -> new Card(rs.getString("key"), rs.getString("path"), rs.getString("icon"), stat(rs.getString("stat_sql"))));
    }

    public boolean enabled(String key) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM content WHERE key=? AND enabled_yn='Y'", Integer.class, key) > 0;
    }

    /** ponytail: SQL-in-DB. 외부 입력 없음, SELECT 만 허용. 불편해지면 ContentStat 인터페이스로 승격. */
    private Long stat(String sql) {
        if (sql == null || !sql.stripLeading().toLowerCase().startsWith("select")) return null;
        try { return jdbc.queryForObject(sql, Long.class); } catch (Exception e) { return null; }
    }
}
