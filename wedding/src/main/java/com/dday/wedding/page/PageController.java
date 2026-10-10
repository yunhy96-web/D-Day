package com.dday.wedding.page;

import com.dday.wedding.common.Valid;
import com.dday.wedding.config.WeddingProps;
import com.dday.wedding.content.ContentService;
import com.dday.wedding.guestbook.GuestbookController;
import com.dday.wedding.label.LabelService;
import com.dday.wedding.photo.PhotoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** `/` 청첩장 + 허브, `/c/{key}` 컨텐츠 페이지 (설계 §4-2). */
@Controller
@RequiredArgsConstructor
public class PageController {
    private static final Set<String> VIEWS = Set.of("worldcup", "guestbook", "gallery", "map");
    private final WeddingProps props;
    private final LabelService labels;
    private final PhotoService photos;
    private final ContentService contents;
    private final GuestbookController guestbook;
    private final ObjectMapper json;

    @GetMapping("/")
    public String index(Model m, @CookieValue(value = "lang", required = false) String langCookie, HttpServletRequest req) {
        common(m, langCookie, req);
        List<String> ids = photos.ids();
        m.addAttribute("previewPhotos", ids.subList(0, Math.min(6, ids.size())).stream().map(photos::url).toList());
        m.addAttribute("photoCount", ids.size());
        m.addAttribute("cards", contents.cards());
        m.addAttribute("guestbookPreview", guestbook.page(0).stream().limit(2).toList());
        return "index";
    }

    @GetMapping("/c/{key}")
    public String content(@PathVariable String key, Model m, @CookieValue(value = "lang", required = false) String langCookie,
                          HttpServletRequest req) throws JsonProcessingException {
        if (!VIEWS.contains(key) || !contents.enabled(key)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        common(m, langCookie, req);
        switch (key) {
            case "gallery" -> m.addAttribute("photos", photos.ids().stream().map(photos::url).toList());
            case "worldcup" -> m.addAttribute("photosJson", json.writeValueAsString(
                    photos.ids().stream().map(id -> Map.of("id", id, "url", photos.url(id))).toList()));
            case "guestbook" -> {
                List<GuestbookController.Entry> rows = guestbook.page(0);
                m.addAttribute("entries", rows.size() > GuestbookController.PAGE ? rows.subList(0, GuestbookController.PAGE) : rows);
                m.addAttribute("hasNext", rows.size() > GuestbookController.PAGE);
            }
            default -> {}
        }
        return "c/" + key;
    }

    private void common(Model m, String langCookie, HttpServletRequest req) {
        String lang = langCookie != null ? Valid.lang(langCookie) : fromAcceptLanguage(req.getHeader("Accept-Language"));
        m.addAttribute("lang", lang);
        m.addAttribute("t", labels.forLang(lang));
        try { m.addAttribute("labelsJson", json.writeValueAsString(labels.all())); } catch (JsonProcessingException e) { m.addAttribute("labelsJson", "{}"); }
        m.addAttribute("p", props);
        m.addAttribute("cspNonce", req.getAttribute("cspNonce"));
    }

    private static String fromAcceptLanguage(String al) {
        if (al == null) return "ko";
        String l = al.toLowerCase();
        return l.startsWith("th") ? "th" : l.startsWith("en") ? "en" : "ko";
    }
}
