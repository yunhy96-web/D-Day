package com.dday.wedding.photo;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/** 설계 §15-6. photo 테이블 = 사진 목록. sort 1번 = 메인. 원본은 서빙하지 않는다. */
@Service
@RequiredArgsConstructor
public class PhotoService {
    private final JdbcTemplate jdbc;

    public record Photo(long id, String view, String thumb, boolean worldcup) {}

    private static Photo row(java.sql.ResultSet rs) throws java.sql.SQLException {
        String f = rs.getString("file");
        return new Photo(rs.getLong("id"), "/photos/view/" + f + ".jpg", "/photos/thumb/" + f + ".jpg", "Y".equals(rs.getString("worldcup_yn")));
    }

    public List<Photo> all() {
        return jdbc.query("SELECT id, file, worldcup_yn FROM photo ORDER BY sort, id", (rs, i) -> row(rs));
    }

    public List<Photo> worldcup() {
        return all().stream().filter(Photo::worldcup).toList();
    }

    /** 메인(첫 화면·og:image). 사진이 없으면 null */
    public Photo main() {
        List<Photo> l = jdbc.query("SELECT id, file, worldcup_yn FROM photo ORDER BY sort, id LIMIT 1", (rs, i) -> row(rs));
        return l.isEmpty() ? null : l.get(0);
    }

    /** 월드컵 pick 의 id 검증 */
    public boolean inWorldcup(String id) {
        if (id == null || !id.matches("\\d{1,9}")) return false;
        return jdbc.queryForObject("SELECT COUNT(*) FROM photo WHERE id=? AND worldcup_yn='Y'", Integer.class, Long.parseLong(id)) > 0;
    }

    public String thumb(String id) {
        if (id == null || !id.matches("\\d{1,9}")) return null;
        List<String> l = jdbc.query("SELECT file FROM photo WHERE id=?", (rs, i) -> "/photos/thumb/" + rs.getString(1) + ".jpg", Long.parseLong(id));
        return l.isEmpty() ? null : l.get(0);
    }
}
