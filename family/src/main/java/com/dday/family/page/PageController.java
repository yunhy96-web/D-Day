package com.dday.family.page;

import com.dday.family.auth.Me;
import com.dday.family.auth.Perm;
import com.dday.family.common.NotFound;
import com.dday.family.media.MediaService.Media;
import com.dday.family.post.PostService;
import com.dday.family.shift.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 화면 라우트 (설계 §5). 데이터는 서버 렌더링, 고치는 동작만 JS → /api. 접근 제한은 AuthInterceptor. */
@Controller
@RequiredArgsConstructor
public class PageController {
    private static final int ALBUM_PAGE = 120;
    private final PostService posts;
    private final ShiftService shifts;
    private final JdbcTemplate jdbc;

    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/signup") public String signup() { return "signup"; }
    @GetMapping("/password") public String password() { return "password"; }
    @GetMapping("/welcome") public String welcome() { return "welcome"; }

    @GetMapping("/pending")
    public String pending(Me me) { return me.active() ? "redirect:/" : "pending"; }

    @GetMapping("/robots.txt")
    @ResponseBody
    public String robots() { return "User-agent: *\nDisallow: /\n"; }

    @GetMapping("/")
    public String feed(@RequestParam(required = false) String kind, @RequestParam(defaultValue = "0") int page, Me me, Model model) {
        String k = "moment".equals(kind) || "diary".equals(kind) ? kind : null;
        pageOf(model, posts.feed(me, k, false, page), page);
        model.addAttribute("kind", k == null ? "all" : k);
        model.addAttribute("today", shifts.today(me.familyId(), LocalDate.now(ZoneId.of(me.timezone())).toString()));
        if (me.has(Perm.MEMBER_APPROVE))
            model.addAttribute("pendingCount", jdbc.queryForObject("SELECT COUNT(*) FROM family_member WHERE family_id=? AND status='PENDING'", Integer.class, me.familyId()));
        return "feed";
    }

    @GetMapping("/diary")
    public String diary(@RequestParam(defaultValue = "0") int page, Me me, Model model) {
        pageOf(model, posts.feed(me, "diary", true, page), page);
        return "diary";
    }

    @GetMapping("/p/{id}")
    public String post(@PathVariable long id, Me me, Model model) {
        PostService.Detail d = posts.detail(me, id);
        boolean mine = d.post().authorId() == me.memberId();
        model.addAttribute("d", d);
        model.addAttribute("mine", mine);
        model.addAttribute("canDelete", mine || me.has(Perm.POST_DELETE_ANY));
        model.addAttribute("canOriginal", mine || me.has(Perm.MEDIA_ORIGINAL));
        return "post";
    }

    @GetMapping("/write")
    public String write(Me me, Model model) {
        me.require(Perm.POST_WRITE);
        model.addAttribute("post", null);
        model.addAttribute("today", LocalDate.now(ZoneId.of(me.timezone())).toString());
        return "write";
    }

    @GetMapping("/p/{id}/edit")
    public String edit(@PathVariable long id, Me me, Model model) {
        PostService.Card c = posts.card(me, id);
        if (c.authorId() != me.memberId()) throw new NotFound();
        model.addAttribute("post", c);
        model.addAttribute("today", LocalDate.now(ZoneId.of(me.timezone())).toString());
        return "write";
    }

    @GetMapping("/album")
    public String album(@RequestParam(defaultValue = "0") int page, Me me, Model model) {
        List<Media> rows = new ArrayList<>(posts.album(me, page, ALBUM_PAGE));
        boolean hasNext = rows.size() > ALBUM_PAGE;
        if (hasNext) rows = rows.subList(0, ALBUM_PAGE);
        Map<String, List<Media>> byMonth = new LinkedHashMap<>();
        for (Media m : rows) byMonth.computeIfAbsent(m.takenAt().substring(0, 7), k -> new ArrayList<>()).add(m);
        model.addAttribute("months", byMonth);
        model.addAttribute("page", page);
        model.addAttribute("hasNext", hasNext);
        return "album";
    }

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) Long member, Me me, Model model) {
        model.addAttribute("today", LocalDate.now(ZoneId.of(me.timezone())).toString());
        model.addAttribute("target", member != null && me.has(Perm.SHIFT_WRITE_ANY) ? member : null);
        model.addAttribute("canWrite", me.has(Perm.SHIFT_WRITE));
        if (me.has(Perm.SHIFT_WRITE_ANY)) model.addAttribute("members", activeMembers(me));
        return "calendar";
    }

    @GetMapping("/me")
    public String mePage(Me me, Model model) {
        Map<String, Object> row = jdbc.queryForMap("SELECT color, legacy_consent_at FROM member WHERE id=?", me.memberId());
        model.addAttribute("color", row.get("color"));
        model.addAttribute("consent", row.get("legacy_consent_at") != null);
        model.addAttribute("colors", com.dday.family.auth.Colors.ALL);
        return "me";
    }

    @GetMapping("/admin")
    public String admin(Me me, Model model) {
        if (!me.admin()) throw new NotFound();
        model.addAttribute("pending", jdbc.queryForList("""
                SELECT m.id, m.login_id, m.name, fm.created_at FROM member m JOIN family_member fm ON fm.member_id=m.id
                WHERE fm.family_id=? AND fm.status='PENDING' ORDER BY fm.created_at""", me.familyId()));
        List<Map<String, Object>> members = jdbc.queryForList("""
                SELECT m.id, m.login_id, m.name, m.color, fm.status, m.last_seen_at,
                       (SELECT GROUP_CONCAT(role_code) FROM member_role r WHERE r.family_id=fm.family_id AND r.member_id=m.id) AS roles
                FROM member m JOIN family_member fm ON fm.member_id=m.id
                WHERE fm.family_id=? AND fm.status<>'PENDING' ORDER BY fm.status, m.id""", me.familyId());
        model.addAttribute("members", members);
        model.addAttribute("roles", jdbc.queryForList("SELECT code FROM role ORDER BY rowid", String.class));
        model.addAttribute("canRoles", me.has(Perm.ROLE_MANAGE));
        model.addAttribute("canManage", me.has(Perm.MEMBER_MANAGE));
        return "admin";
    }

    private List<Map<String, Object>> activeMembers(Me me) {
        return jdbc.queryForList("SELECT m.id, m.name FROM member m JOIN family_member fm ON fm.member_id=m.id WHERE fm.family_id=? AND fm.status='ACTIVE' ORDER BY m.id", me.familyId());
    }

    private static void pageOf(Model model, List<PostService.Card> rows, int page) {
        boolean hasNext = rows.size() > PostService.PAGE;
        model.addAttribute("posts", hasNext ? rows.subList(0, PostService.PAGE) : rows);
        model.addAttribute("hasNext", hasNext);
        model.addAttribute("page", page);
    }
}
