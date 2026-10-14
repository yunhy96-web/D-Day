package com.dday.family.page;

import com.dday.family.auth.AuthInterceptor;
import com.dday.family.auth.AuthService;
import com.dday.family.auth.Cookies;
import com.dday.family.auth.Me;
import com.dday.family.common.Fmt;
import com.dday.family.common.Valid;
import com.dday.family.label.LabelService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.dday.family.common.Forbidden;
import com.dday.family.common.NotFound;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.ZoneId;
import java.util.List;

/**
 * 모든 화면 공통 모델: me, 언어·테마·글씨(로그인 전엔 쿠키), 라벨 L, 날짜 포맷 f, CSP nonce.
 * 언어: member.lang → 쿠키 → Accept-Language(th* 면 th) (설계 §6).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(basePackageClasses = PageController.class)
@RequiredArgsConstructor
public class ViewAdvice {
    private final LabelService labels;
    private final JdbcTemplate jdbc;

    @ModelAttribute
    public void common(Model model, HttpServletRequest req) {
        Me me = (Me) req.getAttribute(AuthInterceptor.ATTR);
        String lang = me != null ? me.lang() : cookie(req, "lang", null);
        if (lang == null) { String al = req.getHeader("Accept-Language"); lang = al != null && al.toLowerCase().startsWith("th") ? "th" : "ko"; }
        String theme = me != null ? me.theme() : cookie(req, "theme", "system");
        String font = me != null ? me.fontScale() : cookie(req, "font", "md");
        model.addAttribute("me", me);
        model.addAttribute("lang", lang);
        model.addAttribute("theme", theme);
        model.addAttribute("font", font);
        model.addAttribute("L", labels.forLang(lang));
        model.addAttribute("Ljson", labels.json(lang));
        model.addAttribute("nonce", req.getAttribute("cspNonce"));
        model.addAttribute("f", new Fmt(lang, font, ZoneId.of(me != null ? me.timezone() : "Asia/Seoul")));
        List<String> fam = jdbc.queryForList("SELECT name FROM family WHERE id=?", String.class, me != null ? me.familyId() : AuthService.DEFAULT_FAMILY);
        model.addAttribute("familyName", fam.isEmpty() ? "" : fam.get(0));
    }

    /** 화면에서 없는 글·권한 없음은 같은 안내 화면 */
    @ExceptionHandler({NotFound.class, Forbidden.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model, HttpServletRequest req) {
        common(model, req);
        return "notfound";
    }

    private static String cookie(HttpServletRequest req, String name, String def) {
        String v = Cookies.get(req, name);
        boolean ok = v != null && switch (name) {
            case "lang" -> Valid.LANGS.contains(v);
            case "theme" -> Valid.THEMES.contains(v);
            default -> Valid.FONTS.contains(v);
        };
        return ok ? v : def;
    }
}
