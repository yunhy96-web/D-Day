package com.dday.wedding.guestbook;

import com.dday.wedding.common.BadRequest;
import com.dday.wedding.common.Images;
import com.dday.wedding.common.Valid;
import com.dday.wedding.config.WeddingProps;
import com.dday.wedding.security.ClientIp;
import com.dday.wedding.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** 설계 §6-1. 익명 + 4자리 비번(BCrypt) + 마스터 비번 + Turnstile(설정 시) + 쿨다운. */
@RestController
public class GuestbookController {
    public static final int PAGE = 10;
    private final JdbcTemplate jdbc;
    private final WeddingProps props;
    private final RateLimiter limiter;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(10);
    private final RestClient http = RestClient.create();
    private final Path photoDir;

    public GuestbookController(JdbcTemplate jdbc, WeddingProps props, RateLimiter limiter, @Value("${app.data-dir}") String dataDir) {
        this.jdbc = jdbc; this.props = props; this.limiter = limiter;
        this.photoDir = Path.of(dataDir, "guestbook");
    }

    public static final int MAX_PHOTOS = 3;
    private static final int MAX_UPLOAD = 4 * 1024 * 1024;

    public record Pic(String thumb, String view) {}
    public record Entry(long id, String name, String message, String createdAt, List<Pic> photos) {}
    public record Write(String sid, String name, String message, String password, String turnstile, List<String> photos) {}
    public record Remove(String password) {}

    public List<Entry> page(int page) {
        List<Entry> rows = jdbc.query("SELECT id, name, message, created_at FROM guestbook WHERE deleted_yn='N' ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, i) -> new Entry(rs.getLong("id"), rs.getString("name"), rs.getString("message"), rs.getString("created_at"), new ArrayList<>()),
                PAGE + 1, page * PAGE);
        if (rows.isEmpty()) return rows;
        Map<Long, Entry> byId = new HashMap<>();
        rows.forEach(e -> byId.put(e.id(), e));
        jdbc.query("SELECT guestbook_id, file FROM guestbook_photo WHERE guestbook_id IN (" + String.join(",", Collections.nCopies(rows.size(), "?")) + ") ORDER BY id",
                rs -> { String f = rs.getString("file");
                        byId.get(rs.getLong("guestbook_id")).photos().add(new Pic("/guestbook-photos/" + f + "_t.jpg", "/guestbook-photos/" + f + ".jpg")); },
                byId.keySet().toArray());
        return rows;
    }

    /** 설계 §15-7. 폰이 2048px 로 줄여 보낸 JPEG 를 서버에서 다시 인코딩(EXIF·GPS 제거) + 썸네일. 글 작성 때 토큰으로 붙인다 */
    @PostMapping(value = "/api/guestbook/photo", consumes = "image/jpeg")
    public Map<String, Object> photo(HttpServletRequest req) throws IOException {
        limiter.check(req, "gbphoto", 10, 600);
        Integer today = jdbc.queryForObject("SELECT COUNT(*) FROM guestbook_photo WHERE created_at > datetime('now','-1 day')", Integer.class);
        if (today >= 500) throw new RateLimiter.TooManyRequests();
        removeOrphans();
        BufferedImage img = Images.decodeJpeg(Images.read(req.getInputStream(), MAX_UPLOAD), 2600);
        String token = UUID.randomUUID().toString();
        Images.save(photoDir, token + "_t.jpg", Images.encodeJpeg(img, 600, 0.8f));
        Images.save(photoDir, token + ".jpg", Images.encodeJpeg(img, 2048, 0.88f));
        jdbc.update("INSERT INTO guestbook_photo(file) VALUES (?)", token);
        return Map.of("token", token);
    }

    /** 올리고 글은 안 쓴 사진: 하루 지나면 정리 */
    private void removeOrphans() throws IOException {
        List<String> old = jdbc.query("SELECT file FROM guestbook_photo WHERE guestbook_id IS NULL AND created_at < datetime('now','-1 day')", (rs, i) -> rs.getString(1));
        for (String f : old) {
            Files.deleteIfExists(photoDir.resolve(f + ".jpg"));
            Files.deleteIfExists(photoDir.resolve(f + "_t.jpg"));
        }
        if (!old.isEmpty()) jdbc.update("DELETE FROM guestbook_photo WHERE guestbook_id IS NULL AND created_at < datetime('now','-1 day')");
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
        List<String> tokens = w.photos() == null ? List.of() : w.photos().stream().map(Valid::sid).distinct().toList();   // 토큰도 UUID
        if (tokens.size() > MAX_PHOTOS) throw new BadRequest("photos");
        verifyTurnstile(w.turnstile(), ClientIp.of(req));
        // last_insert_rowid() 는 커넥션 풀에서 다른 커넥션을 탈 수 있어 RETURNING 사용
        Long id = jdbc.queryForObject("INSERT INTO guestbook(name, message, pw_hash) VALUES (?,?,?) RETURNING id", Long.class, name, msg, bcrypt.encode(w.password()));
        tokens.forEach(t -> jdbc.update("UPDATE guestbook_photo SET guestbook_id=? WHERE file=? AND guestbook_id IS NULL", id, t));
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
