package com.dday.family.member;

import com.dday.family.auth.AuthService;
import com.dday.family.auth.Cookies;
import com.dday.family.auth.Me;
import com.dday.family.common.BadRequest;
import com.dday.family.common.Times;
import com.dday.family.common.Valid;
import com.dday.family.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 내 정보. 필드 하나씩 와도 된다(자동 저장, 설계 §7-2). 언어·테마·글씨는 /api/prefs */
@RestController
@RequiredArgsConstructor
public class MeController {
    private final JdbcTemplate jdbc;
    private final AuthService auth;
    private final Cookies cookies;
    private final RateLimiter limiter;

    public record Profile(String name, String color, Boolean legacyConsent) {}
    public record Password(String current, String next) {}

    @PutMapping("/api/me")
    public ResponseEntity<Void> profile(@RequestBody Profile b, Me me) {
        if (b.name() != null) jdbc.update("UPDATE member SET name=? WHERE id=?", Valid.text(b.name(), 20, "name"), me.memberId());
        if (b.color() != null) jdbc.update("UPDATE member SET color=? WHERE id=?", Valid.oneOf(b.color(), Valid.COLORS, "color"), me.memberId());
        if (b.legacyConsent() != null)
            jdbc.update("UPDATE member SET legacy_consent_at=? WHERE id=?", b.legacyConsent() ? Times.now() : null, me.memberId());
        return ResponseEntity.noContent().build();
    }

    /** 바꾸면 다른 기기 세션은 모두 끊고 지금 기기는 새 세션 */
    @PutMapping("/api/me/password")
    public ResponseEntity<Void> password(@RequestBody Password b, Me me, HttpServletRequest req, HttpServletResponse res) {
        limiter.check(req, "password", 10, 600);
        String hash = jdbc.queryForObject("SELECT password_hash FROM member WHERE id=?", String.class, me.memberId());
        if (b.current() == null || !AuthService.matches(b.current(), hash)) throw new BadRequest("currentPassword");
        String next = Valid.password(b.next());
        if (next.equals(b.current())) throw new BadRequest("samePassword");
        jdbc.update("UPDATE member SET password_hash=?, must_change_pw='N' WHERE id=?", AuthService.hash(next), me.memberId());
        auth.killSessions(me.memberId());
        cookies.session(res, auth.newSession(me.memberId(), me.familyId()));
        return ResponseEntity.noContent().build();
    }
}
