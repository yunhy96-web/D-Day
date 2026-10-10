package com.dday.wedding.worldcup;

import com.dday.wedding.common.BadRequest;
import com.dday.wedding.common.Valid;
import com.dday.wedding.photo.PhotoService;
import com.dday.wedding.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 설계 §5. 대진은 클라이언트, 서버는 pick 기록·집계만. */
@RestController
@RequiredArgsConstructor
public class WorldcupController {
    private static final int[] CAP = {0, 16, 8, 4, 2, 1};   // round 별 sid 당 pick 상한
    private final JdbcTemplate jdbc;
    private final PhotoService photos;
    private final RateLimiter limiter;

    public record Pick(String sid, Integer round, String winnerId, String loserId) {}

    @PostMapping("/api/pick")
    public ResponseEntity<Void> pick(@RequestBody Pick p, HttpServletRequest req) {
        limiter.check(req, "pick", 40, 60);
        String sid = Valid.sid(p.sid());
        if (p.round() == null || p.round() < 1 || p.round() > 5) throw new BadRequest("round");
        if (p.winnerId() == null || p.loserId() == null || p.winnerId().equals(p.loserId())
                || !photos.exists(p.winnerId()) || !photos.exists(p.loserId())) throw new BadRequest("photo");
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM pick WHERE session_id=? AND round=?", Integer.class, sid, p.round());
        if (n >= CAP[p.round()]) return ResponseEntity.noContent().build();   // 초과는 조용히 무시
        jdbc.update("INSERT INTO pick(session_id, round, winner_id, loser_id) VALUES (?,?,?,?)", sid, p.round(), p.winnerId(), p.loserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/ranking")
    public Map<String, Object> ranking() {
        List<Map<String, Object>> top = jdbc.query(
                "SELECT winner_id AS id, COUNT(*) AS wins FROM pick WHERE round=5 GROUP BY winner_id ORDER BY wins DESC, id LIMIT 3",
                (rs, i) -> Map.of("id", rs.getString("id"), "wins", rs.getLong("wins"), "url", String.valueOf(photos.url(rs.getString("id")))));
        Long participants = jdbc.queryForObject("SELECT COUNT(DISTINCT session_id) FROM pick WHERE round=5", Long.class);
        return Map.of("participants", participants, "top", top);
    }
}
