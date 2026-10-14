package com.dday.family.auth;

import com.dday.family.common.BadRequest;
import com.dday.family.common.Valid;
import com.dday.family.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/** 로그인·가입·나가기·화면 설정 (설계 §4·§10). 전부 공개 경로. */
@RestController
@RequiredArgsConstructor
public class AuthController {
    private static final Duration PREF_AGE = Duration.ofDays(365);
    private final AuthService auth;
    private final Cookies cookies;
    private final RateLimiter limiter;
    private final Audit audit;
    private final JdbcTemplate jdbc;

    public record Login(String loginId, String password) {}
    public record Signup(String loginId, String password, String name) {}
    public record Prefs(String lang, String theme, String fontScale, String timezone) {}

    @PostMapping("/api/login")
    public Map<String, String> login(@RequestBody Login b, HttpServletRequest req, HttpServletResponse res) {
        limiter.check(req, "login", 10, 600);
        String id = b.loginId() == null ? "" : b.loginId().strip().toLowerCase();
        AuthService.LoginResult r = auth.login(id, b.password() == null ? "" : b.password());
        if (r == null) {
            audit.log(null, null, "login.fail", "loginId:" + id.substring(0, Math.min(20, id.length())), req);
            throw new BadRequest("loginFail");
        }
        cookies.session(res, r.token());
        return Map.of("next", "ACTIVE".equals(r.status()) ? "/" : "/pending");
    }

    @PostMapping("/api/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        auth.logout(Cookies.get(req, Cookies.SESSION));
        cookies.clearSession(res);
        return ResponseEntity.noContent().build();
    }

    /** 가입 신청 → PENDING + 바로 로그인(대기 화면을 보여주려고). 화면 설정은 지금 쿠키 값을 따라감 */
    @PostMapping("/api/signup")
    public Map<String, String> signup(@RequestBody Signup b, HttpServletRequest req, HttpServletResponse res) {
        limiter.check(req, "signup", 3, 3600);
        String loginId = Valid.loginId(b.loginId());
        String pw = Valid.password(b.password());
        String name = Valid.text(b.name(), 20, "name");
        long id = auth.signup(loginId, pw, name, pref(req, "lang", Valid.LANGS, "ko"), pref(req, "theme", Valid.THEMES, "system"),
                pref(req, "font", Valid.FONTS, "md"));
        audit.log(AuthService.DEFAULT_FAMILY, id, "member.signup", "member:" + id, req);
        cookies.session(res, auth.newSession(id, AuthService.DEFAULT_FAMILY));
        return Map.of("next", "/pending");
    }

    /** 언어·테마·글씨 크기·시간대. 바꾸는 즉시 저장(설계 §7-2). 로그인 전엔 쿠키만 */
    @PutMapping("/api/prefs")
    public ResponseEntity<Void> prefs(@RequestBody Prefs b, Me me, HttpServletResponse res) {
        if (b.lang() != null) {
            String v = Valid.oneOf(b.lang(), Valid.LANGS, "lang");
            cookies.set(res, "lang", v, PREF_AGE);
            if (me != null) jdbc.update("UPDATE member SET lang=? WHERE id=?", v, me.memberId());
        }
        if (b.theme() != null) {
            String v = Valid.oneOf(b.theme(), Valid.THEMES, "theme");
            cookies.set(res, "theme", v, PREF_AGE);
            if (me != null) jdbc.update("UPDATE member SET theme=? WHERE id=?", v, me.memberId());
        }
        if (b.fontScale() != null) {
            String v = Valid.oneOf(b.fontScale(), Valid.FONTS, "fontScale");
            cookies.set(res, "font", v, PREF_AGE);
            if (me != null) jdbc.update("UPDATE member SET font_scale=?, font_scale_set_yn='Y' WHERE id=?", v, me.memberId());
        }
        if (b.timezone() != null && me != null)
            jdbc.update("UPDATE member SET timezone=? WHERE id=?", Valid.oneOf(b.timezone(), Valid.TIMEZONES, "timezone"), me.memberId());
        return ResponseEntity.noContent().build();
    }

    private static String pref(HttpServletRequest req, String cookie, java.util.Set<String> allowed, String def) {
        String v = Cookies.get(req, cookie);
        return v != null && allowed.contains(v) ? v : def;
    }
}
