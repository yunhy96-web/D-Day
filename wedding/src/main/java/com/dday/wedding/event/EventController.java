package com.dday.wedding.event;

import com.dday.wedding.common.BadRequest;
import com.dday.wedding.common.Valid;
import com.dday.wedding.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/** 가벼운 퍼널 분석 (설계 §4-1). name 은 화이트리스트, meta 는 200자 이하. */
@RestController
@RequiredArgsConstructor
public class EventController {
    static final Set<String> NAMES = Set.of("share.in", "worldcup.start", "worldcup.finish", "worldcup.share",
            "guestbook.write", "gallery.open", "map.navigate", "account.copy", "lang.switch", "content.open");
    private final JdbcTemplate jdbc;
    private final RateLimiter limiter;

    @PostMapping("/api/event")
    public ResponseEntity<Void> log(@RequestBody Map<String, String> body, HttpServletRequest req) {
        limiter.check(req, "ev", 20, 60);
        String sid = Valid.sid(body.get("sid"));
        String name = body.get("name");
        if (name == null || !NAMES.contains(name)) throw new BadRequest("name");
        String meta = body.get("meta");
        if (meta != null && meta.length() > 200) throw new BadRequest("meta");
        jdbc.update("INSERT INTO event(session_id, name, meta) VALUES (?,?,?)", sid, name, meta);
        return ResponseEntity.noContent().build();
    }
}
