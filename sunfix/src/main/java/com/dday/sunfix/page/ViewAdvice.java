package com.dday.sunfix.page;

import com.dday.sunfix.admin.AdminController;
import com.dday.sunfix.common.NotFound;
import com.dday.sunfix.common.Valid;
import com.dday.sunfix.label.LabelService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Duration;
import java.util.Map;

/**
 * 모든 화면 공통 모델: 언어·테마, 라벨 L·Ljson, 표시 헬퍼 f, CSP nonce.
 * 언어: ?lang= (쿠키로 저장) → 쿠키 lang → Accept-Language(ko* 면 ko, 그 외 en) (설계 §6).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(basePackageClasses = {PageController.class, AdminController.class})
@RequiredArgsConstructor
public class ViewAdvice {
    private final LabelService labels;

    @ModelAttribute
    public void common(Model model, HttpServletRequest req, HttpServletResponse res) {
        String lang = req.getParameter("lang");
        if (lang != null && Valid.LANGS.contains(lang))
            res.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("lang", lang).path("/").sameSite("Lax").maxAge(Duration.ofDays(365)).build().toString());
        else lang = cookie(req, "lang");
        if (lang == null || !Valid.LANGS.contains(lang)) {
            String al = req.getHeader("Accept-Language");
            lang = al == null || al.toLowerCase().startsWith("ko") ? "ko" : "en";
        }
        String theme = cookie(req, "theme");
        Map<String, String> L = labels.forLang(lang);
        model.addAttribute("lang", lang);
        model.addAttribute("theme", theme != null && Valid.THEMES.contains(theme) ? theme : "system");
        model.addAttribute("L", L);
        model.addAttribute("Ljson", labels.json(lang));
        model.addAttribute("f", new Fmt(lang, L));
        model.addAttribute("nonce", req.getAttribute("cspNonce"));
        model.addAttribute("path", req.getRequestURI());
    }

    @ExceptionHandler(NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model, HttpServletRequest req, HttpServletResponse res) {
        common(model, req, res);
        return "notfound";
    }

    private static String cookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) if (c.getName().equals(name)) return c.getValue();
        return null;
    }
}
