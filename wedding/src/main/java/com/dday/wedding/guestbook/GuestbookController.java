package com.dday.wedding.guestbook;

import com.dday.wedding.common.BadRequest;
import com.dday.wedding.common.Valid;
import com.dday.wedding.config.WeddingProps;
import com.dday.wedding.security.ClientIp;
import com.dday.wedding.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** 설계 §6-1. 익명 + 4자리 비번(BCrypt) + 마스터 비번 + Turnstile(설정 시) + 쿨다운. */
@RestController
@RequiredArgsConstructor
public class GuestbookController {
    public static final int PAGE = 10;
    private final JdbcTemplate jdbc;
    private final WeddingProps props;
    private final RateLimiter limiter;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(10);
    private final RestClient http = RestClient.create();

    public record Entry(long id, String name, String message, String createdAt) {}
    public record Write(String sid, String name, String message, String password, String turnstile) {}
    public record Remove(String password) {}

    public List<Entry> page(int page) {
        return jdbc.query("SELECT id, name, message, created_at FROM guestbook WHERE deleted_yn='N' ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, i) -> new Entry(rs.getLong("id"), rs.getString("name"), rs.getString("message"), rs.getString("created_at")),
                PAGE + 1, page * PAGE);
    }

    @GetMapping("/api/guestbook")
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page) {
        List<Entry> rows = page(Math.max(0, page));
        boolean hasNext = rows.size() > PAGE;
        return Map.of("items", hasNext ? rows.subList(0, PAGE) : rows, "hasNext", hasNext);
    }

    @PostMapping("/api/guestbook")
    public Map<String, Object> write(@RequestBody Write w, HttpServletRequest req) {
        limiter.check(req, "gb", 1, 30);
        String sid = Valid.sid(w.sid());
        String name = Valid.text(w.name(), 20, "name");
        String msg = Valid.text(w.message(), 300, "message");
        if (w.password() == null || !w.password().matches("\\d{4}")) throw new BadRequest("password");
        verifyTurnstile(w.turnstile(), ClientIp.of(req));
        // last_insert_rowid() 는 커넥션 풀에서 다른 커넥션을 탈 수 있어 RETURNING 사용
        Long id = jdbc.queryForObject("INSERT INTO guestbook(name, message, pw_hash) VALUES (?,?,?) RETURNING id", Long.class, name, msg, bcrypt.encode(w.password()));
        jdbc.update("INSERT INTO event(session_id, name) VALUES (?, 'guestbook.write')", sid);
        return Map.of("id", id);
    }

    @DeleteMapping("/api/guestbook/{id}")
    public ResponseEntity<Void> remove(@PathVariable long id, @RequestBody Remove r, HttpServletRequest req) {
        limiter.check(req, "gbdel", 5, 600);
        String pw = r.password() == null ? "" : r.password();
        String hash = jdbc.query("SELECT pw_hash FROM guestbook WHERE id=? AND deleted_yn='N'", rs -> rs.next() ? rs.getString(1) : null, id);
        if (hash == null) return ResponseEntity.notFound().build();
        boolean ok = bcrypt.matches(pw, hash) || (!props.getMasterPwHash().isBlank() && bcrypt.matches(pw, props.getMasterPwHash()));
        if (!ok) return ResponseEntity.status(403).build();
        jdbc.update("UPDATE guestbook SET deleted_yn='Y' WHERE id=?", id);
        return ResponseEntity.noContent().build();
    }

    private void verifyTurnstile(String token, String ip) {
        if (props.getTurnstileSecret().isBlank()) return;   // 미설정(로컬) 이면 생략
        if (token == null || token.isBlank()) throw new BadRequest("turnstile");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", props.getTurnstileSecret()); form.add("response", token); form.add("remoteip", ip);
        Map<?, ?> res = http.post().uri("https://challenges.cloudflare.com/turnstile/v0/siteverify").body(form).retrieve().body(Map.class);
        if (res == null || !Boolean.TRUE.equals(res.get("success"))) throw new BadRequest("turnstile");
    }
}
