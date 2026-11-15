package com.dday.wedding.photo;

import com.dday.wedding.common.ObjectStore;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/** 설계 §15-6. photo 테이블 = 사진 목록. sort 1번 = 메인. 원본은 서빙하지 않는다. */
@Service
@RequiredArgsConstructor
public class PhotoService {
    private final JdbcTemplate jdbc;
    private final ObjectStore store;

    public record Photo(long id, String view, String thumb, boolean worldcup, boolean visible) {}
    private static final String SELECT = "SELECT p.id, p.file, p.worldcup_yn, h.photo_id IS NULL AS visible FROM photo p LEFT JOIN photo_hidden h ON h.photo_id = p.id ";

    private Photo row(java.sql.ResultSet rs) throws java.sql.SQLException {
        String f = rs.getString("file");
        return new Photo(rs.getLong("id"), store.url(viewKey(f)), store.url(thumbKey(f)), "Y".equals(rs.getString("worldcup_yn")), rs.getBoolean("visible"));
    }

    public static String viewKey(String file) { return "photos/view/" + file + ".jpg"; }
    public static String thumbKey(String file) { return "photos/thumb/" + file + ".jpg"; }

    /** 하객 화면용: 숨긴 사진 제외 */
    public List<Photo> all() {
        return jdbc.query(SELECT + "WHERE h.photo_id IS NULL ORDER BY p.sort, p.id", (rs, i) -> row(rs));
    }

    /** 어드민용: 숨긴 사진 포함 */
    public List<Photo> adminAll() {
        return jdbc.query(SELECT + "ORDER BY p.sort, p.id", (rs, i) -> row(rs));
    }

    public List<Photo> worldcup() {
        return all().stream().filter(Photo::worldcup).toList();
    }

    /** 메인(첫 화면·og:image). 사진이 없으면 null */
    public Photo main() {
        List<Photo> l = jdbc.query(SELECT + "WHERE h.photo_id IS NULL ORDER BY p.sort, p.id LIMIT 1", (rs, i) -> row(rs));
        return l.isEmpty() ? null : l.get(0);
    }

    /** 월드컵 pick 의 id 검증 */
    public boolean inWorldcup(String id) {
        if (id == null || !id.matches("\\d{1,9}")) return false;
        return jdbc.queryForObject("SELECT COUNT(*) FROM photo WHERE id=? AND worldcup_yn='Y' AND id NOT IN (SELECT photo_id FROM photo_hidden)", Integer.class, Long.parseLong(id)) > 0;
    }

    public String thumb(String id) {
        if (id == null || !id.matches("\\d{1,9}")) return null;
        List<String> l = jdbc.query("SELECT file FROM photo WHERE id=?", (rs, i) -> store.url(thumbKey(rs.getString(1))), Long.parseLong(id));
        return l.isEmpty() ? null : l.get(0);
    }
}
