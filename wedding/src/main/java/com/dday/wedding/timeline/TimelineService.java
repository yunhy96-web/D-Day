package com.dday.wedding.timeline;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/** 설계 §15-3. 사진 파일은 data/timeline, 메타는 timeline 테이블. */
@Service
@RequiredArgsConstructor
public class TimelineService {
    private final JdbcTemplate jdbc;

    public record Item(long id, String month, String url, String caption) {}

    public List<Item> items() {
        return jdbc.query("SELECT id, month, file, caption FROM timeline ORDER BY month, id",
                (rs, i) -> new Item(rs.getLong("id"), rs.getString("month"), "/timeline/" + rs.getString("file"), rs.getString("caption")));
    }
}
