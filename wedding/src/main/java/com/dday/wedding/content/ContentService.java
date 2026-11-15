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
        // stat 쿼리는 바깥 쿼리가 끝난 뒤에. 콜백 안에서 부르면 커넥션 2개를 동시에 잡아, 동시 접속 4명 이상이면 풀(4)이 교착된다
        List<String[]> rows = jdbc.query("SELECT key, path, icon, stat_sql FROM content WHERE enabled_yn='Y' ORDER BY sort",
                (rs, i) -> new String[]{rs.getString("key"), rs.getString("path"), rs.getString("icon"), rs.getString("stat_sql")});
        return rows.stream().map(r -> new Card(r[0], r[1], r[2], stat(r[3]))).toList();
    }

    /** 페이지 접근 가능 여부. enabled_yn 은 허브 카드 노출만 제어한다 */
    public boolean exists(String key) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM content WHERE key=?", Integer.class, key) > 0;
    }

    /** ponytail: SQL-in-DB. 외부 입력 없음, SELECT 만 허용. 불편해지면 ContentStat 인터페이스로 승격. */
    private Long stat(String sql) {
        if (sql == null || !sql.stripLeading().toLowerCase().startsWith("select")) return null;
        try { return jdbc.queryForObject(sql, Long.class); } catch (Exception e) { return null; }
    }
}
