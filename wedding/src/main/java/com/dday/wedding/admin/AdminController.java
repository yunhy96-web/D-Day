package com.dday.wedding.admin;

import com.dday.wedding.common.BadRequest;
import com.dday.wedding.common.Images;
import com.dday.wedding.common.ObjectStore;
import com.dday.wedding.common.Valid;
import com.dday.wedding.config.WeddingProps;
import com.dday.wedding.security.RateLimiter;
import com.dday.wedding.timeline.TimelineService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 설계 §15-1. 나 혼자 쓰는 어드민: 퀴즈 CRUD + 타임라인 사진. 인증은 AdminInterceptor. */
@RestController
public class AdminController {
    private static final int MAX_IMAGE = 8 * 1024 * 1024;
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final WeddingProps props;
    private final RateLimiter limiter;
    private final TimelineService timeline;
    private final ObjectMapper json;
    private final ObjectStore store;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(10);

    public AdminController(JdbcTemplate jdbc, WeddingProps props, RateLimiter limiter, TimelineService timeline, ObjectMapper json, ObjectStore store) {
        this.jdbc = jdbc; this.props = props; this.limiter = limiter; this.timeline = timeline; this.json = json; this.store = store;
    }

    public record Login(String username, String password) {}
    public record Quiz(Long id, String question, List<String> choices, Integer answerNo, Integer points, String hint) {}
    public record TimelineEdit(String month, String caption) {}

    @GetMapping("/admin")
    public ModelAndView page(HttpServletRequest req) {
        return new ModelAndView("admin", Map.of("loggedIn", AdminInterceptor.loggedIn(req),
                "cspNonce", req.getAttribute("cspNonce"), "monthMin", props.getDatingStart().substring(0, 7),
                "monthMax", props.getDateTime().substring(0, 7)));
    }

    @PostMapping("/admin/api/login")
    public ResponseEntity<Void> login(@RequestBody Login l, HttpServletRequest req) {
        limiter.check(req, "admin", 5, 600);
        String hash = props.getAdminPwHash();
        if (hash.isBlank() || !props.getAdminId().equals(l.username()) || l.password() == null || !bcrypt.matches(l.password(), hash)) return ResponseEntity.status(403).build();
        req.getSession(true);
        req.changeSessionId();   // 세션 고정 방지
        req.getSession().setAttribute(AdminInterceptor.ATTR, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/api/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
        return ResponseEntity.noContent().build();
    }

    // ---- 퀴즈

    @GetMapping("/admin/api/quiz")
    public List<Quiz> quizzes() {
        return jdbc.query("SELECT id, question, choices, answer_no, points, hint FROM quiz WHERE deleted_yn='N' ORDER BY points, id",
                (rs, i) -> new Quiz(rs.getLong("id"), rs.getString("question"), parse(rs.getString("choices")),
                        rs.getInt("answer_no"), rs.getInt("points"), rs.getString("hint")));
    }

    @PostMapping("/admin/api/quiz")
    public Map<String, Object> createQuiz(@RequestBody Quiz q) throws JsonProcessingException {
        Quiz v = valid(q);
        Long id = jdbc.queryForObject("INSERT INTO quiz(question, choices, answer_no, points, hint) VALUES (?,?,?,?,?) RETURNING id", Long.class,
                v.question(), json.writeValueAsString(v.choices()), v.answerNo(), v.points(), v.hint());
        return Map.of("id", id);
    }

    @PutMapping("/admin/api/quiz/{id}")
    public ResponseEntity<Void> updateQuiz(@PathVariable long id, @RequestBody Quiz q) throws JsonProcessingException {
        Quiz v = valid(q);
        int n = jdbc.update("UPDATE quiz SET question=?, choices=?, answer_no=?, points=?, hint=? WHERE id=? AND deleted_yn='N'",
                v.question(), json.writeValueAsString(v.choices()), v.answerNo(), v.points(), v.hint(), id);
        return n == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }

    /** 이미 출제된 응시의 채점이 깨지지 않도록 소프트 삭제 */
    @DeleteMapping("/admin/api/quiz/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable long id) {
        int n = jdbc.update("UPDATE quiz SET deleted_yn='Y' WHERE id=? AND deleted_yn='N'", id);
        return n == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }

    private Quiz valid(Quiz q) {
        String question = Valid.text(q.question(), 200, "question");
        if (q.choices() == null || q.choices().size() < 2 || q.choices().size() > 4) throw new BadRequest("choices");
        List<String> choices = q.choices().stream().map(c -> Valid.text(c, 80, "choices")).toList();
        if (q.answerNo() == null || q.answerNo() < 0 || q.answerNo() >= choices.size()) throw new BadRequest("answerNo");
        if (q.points() == null || q.points() < 1 || q.points() > 5) throw new BadRequest("points");
        String hint = q.hint() == null || q.hint().isBlank() ? null : Valid.text(q.hint(), 200, "hint");
        return new Quiz(null, question, choices, q.answerNo(), q.points(), hint);
    }

    private List<String> parse(String choices) {
        try { return json.readValue(choices, STRINGS); } catch (JsonProcessingException e) { return List.of(); }
    }

    // ---- 타임라인

    @GetMapping("/admin/api/timeline")
    public List<TimelineService.Item> timelineItems() { return timeline.items(); }

    /** 브라우저가 2560px JPEG 로 줄여서 body 로 보낸다. 8MB 상한 + 실제 JPEG 디코딩 확인 */
    @PostMapping(value = "/admin/api/timeline", consumes = "image/jpeg")
    public Map<String, Object> upload(@RequestParam String month, @RequestParam(required = false) String caption,
                                      HttpServletRequest req) throws IOException {
        String m = month(month);
        String c = caption(caption);
        byte[] img = Images.read(req.getInputStream(), MAX_IMAGE);
        Images.decodeJpeg(img, 2600);
        String file = UUID.randomUUID() + ".jpg";
        store.put(TimelineService.key(file), img);
        Long id = jdbc.queryForObject("INSERT INTO timeline(month, file, caption) VALUES (?,?,?) RETURNING id", Long.class, m, file, c);
        return Map.of("id", id);
    }

    @PutMapping("/admin/api/timeline/{id}")
    public ResponseEntity<Void> updateTimeline(@PathVariable long id, @RequestBody TimelineEdit e) {
        int n = jdbc.update("UPDATE timeline SET month=?, caption=? WHERE id=?", month(e.month()), caption(e.caption()), id);
        return n == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/api/timeline/{id}")
    public ResponseEntity<Void> deleteTimeline(@PathVariable long id) throws IOException {
        String file = jdbc.query("SELECT file FROM timeline WHERE id=?", rs -> rs.next() ? rs.getString(1) : null, id);
        if (file == null) return ResponseEntity.notFound().build();
        jdbc.update("DELETE FROM timeline WHERE id=?", id);
        store.delete(TimelineService.key(file));
        return ResponseEntity.noContent().build();
    }

    /** 'YYYY-MM', 연애 시작 월 ~ 예식 월 */
    private String month(String m) {
        if (m == null || !m.matches("\\d{4}-(0[1-9]|1[0-2])")) throw new BadRequest("month");
        if (m.compareTo(props.getDatingStart().substring(0, 7)) < 0 || m.compareTo(props.getDateTime().substring(0, 7)) > 0) throw new BadRequest("month");
        return m;
    }

    private String caption(String c) {
        return c == null || c.isBlank() ? null : Valid.text(c, 60, "caption");
    }
}
