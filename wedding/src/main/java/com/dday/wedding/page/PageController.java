package com.dday.wedding.page;

import com.dday.wedding.common.Valid;
import com.dday.wedding.config.WeddingProps;
import com.dday.wedding.content.ContentService;
import com.dday.wedding.guestbook.GuestbookController;
import com.dday.wedding.label.LabelService;
import com.dday.wedding.photo.PhotoService;
import com.dday.wedding.timeline.TimelineService;
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
    private static final Set<String> VIEWS = Set.of("worldcup", "guestbook", "gallery", "map", "quiz", "timeline");
    private final WeddingProps props;
    private final LabelService labels;
    private final PhotoService photos;
    private final ContentService contents;
    private final GuestbookController guestbook;
    private final TimelineService timeline;
    private final ObjectMapper json;

    @GetMapping("/")
    public String index(Model m, @CookieValue(value = "lang", required = false) String langCookie, HttpServletRequest req) {
        common(m, langCookie, req);
        List<PhotoService.Photo> all = photos.all();
        // 1번은 첫 화면(메인)이라 미리보기는 2번부터 6장
        m.addAttribute("previewPhotos", all.stream().skip(1).limit(6).map(PhotoService.Photo::thumb).toList());
        m.addAttribute("photoCount", all.size());
        m.addAttribute("cards", contents.cards());
        m.addAttribute("guestbookPreview", guestbook.page(0).stream().limit(2).toList());
        return "index";
    }

    @GetMapping("/c/{key}")
    public String content(@PathVariable String key, Model m, @CookieValue(value = "lang", required = false) String langCookie,
                          HttpServletRequest req) throws JsonProcessingException {
        if (!VIEWS.contains(key) || !contents.exists(key)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        common(m, langCookie, req);
        switch (key) {
            case "gallery" -> m.addAttribute("photos", photos.all());
            case "worldcup" -> m.addAttribute("photosJson", json.writeValueAsString(photos.worldcup().stream()
                    .map(ph -> Map.of("id", String.valueOf(ph.id()), "url", ph.view(), "thumb", ph.thumb())).toList()));
            case "guestbook" -> {
                List<GuestbookController.Entry> rows = guestbook.page(0);
                m.addAttribute("entries", rows.size() > GuestbookController.PAGE ? rows.subList(0, GuestbookController.PAGE) : rows);
                m.addAttribute("hasNext", rows.size() > GuestbookController.PAGE);
            }
            // 캡션은 어드민 입력이지만 <script> 안에 임베드되므로 '<' 를 이스케이프
            case "timeline" -> m.addAttribute("timelineJson", json.writeValueAsString(timeline.items()).replace("<", "\\u003c"));
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
        PhotoService.Photo main = photos.main();
        m.addAttribute("mainPhoto", main == null ? null : main.view());   // 첫 화면 + og:image
        m.addAttribute("cspNonce", req.getAttribute("cspNonce"));
    }

    private static String fromAcceptLanguage(String al) {
        if (al == null) return "ko";
        String l = al.toLowerCase();
        return l.startsWith("th") ? "th" : l.startsWith("en") ? "en" : "ko";
    }
}
