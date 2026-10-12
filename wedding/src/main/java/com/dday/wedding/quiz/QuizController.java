package com.dday.wedding.quiz;

import com.dday.wedding.common.BadRequest;
import com.dday.wedding.common.Valid;
import com.dday.wedding.security.RateLimiter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** 설계 §15-2. 정답은 절대 내려보내지 않는다. 채점은 응시(attempt) 1회 단위로 서버에서. */
@RestController
@RequiredArgsConstructor
public class QuizController {
    public static final int SIZE = 10;
    /** 난이도 → 출제 배점 범위. hard 의 4점은 normal 과 의도적으로 겹침 */
    static final Map<String, int[]> LEVELS = Map.of("easy", new int[]{1, 2}, "normal", new int[]{3, 4}, "hard", new int[]{4, 5});
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final RateLimiter limiter;
    private final ObjectMapper json;

    public record Start(String sid, String level) {}
    public record Answer(Long id, Integer no) {}
    public record Submit(List<Answer> answers) {}

    @PostMapping("/api/quiz/start")
    public Map<String, Object> start(@RequestBody Start s, HttpServletRequest req) {
        limiter.check(req, "quiz", 20, 600);
        String sid = Valid.sid(s.sid());
        int[] range = LEVELS.get(s.level());
        if (range == null) throw new BadRequest("level");
        List<Map<String, Object>> items = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        int[] max = {0};
        jdbc.query("SELECT id, question, choices, points, hint FROM quiz WHERE deleted_yn='N' AND points BETWEEN ? AND ? ORDER BY random() LIMIT ?",
                rs -> {
                    List<String> choices = parse(rs.getString("choices"));
                    List<Map<String, Object>> shuffled = IntStream.range(0, choices.size())
                            .mapToObj(i -> Map.<String, Object>of("no", i, "text", choices.get(i)))
                            .collect(Collectors.toCollection(ArrayList::new));
                    Collections.shuffle(shuffled);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getLong("id"));
                    item.put("question", rs.getString("question"));
                    item.put("points", rs.getInt("points"));
                    item.put("choices", shuffled);
                    String hint = rs.getString("hint");
                    if (hint != null && !hint.isBlank()) item.put("hint", hint);   // 화면엔 안 그림. 개발자 도구용
                    items.add(item); ids.add(rs.getLong("id")); max[0] += rs.getInt("points");
                }, range[0], range[1], SIZE);
        String attemptId = UUID.randomUUID().toString();
        if (!ids.isEmpty())
            jdbc.update("INSERT INTO quiz_attempt(id, session_id, level, quiz_ids, max_score) VALUES (?,?,?,?,?)",
                    attemptId, sid, s.level(), ids.stream().map(String::valueOf).collect(Collectors.joining(",")), max[0]);
        return Map.of("attemptId", attemptId, "items", items);
    }

    @PostMapping("/api/quiz/{attemptId}/submit")
    public ResponseEntity<Map<String, Object>> submit(@PathVariable String attemptId, @RequestBody Submit s, HttpServletRequest req) {
        limiter.check(req, "quizsub", 20, 600);
        String aid = Valid.sid(attemptId);
        String quizIds = jdbc.query("SELECT quiz_ids FROM quiz_attempt WHERE id=? AND submitted_at IS NULL", rs -> rs.next() ? rs.getString(1) : null, aid);
        if (quizIds == null) return ResponseEntity.status(409).build();   // 없는 응시이거나 이미 제출
        Map<Long, Integer> picked = new HashMap<>();
        if (s.answers() != null) s.answers().stream().filter(a -> a != null && a.id() != null && a.no() != null).forEach(a -> picked.put(a.id(), a.no()));

        List<Long> ids = Arrays.stream(quizIds.split(",")).map(Long::valueOf).toList();
        Map<Long, int[]> key = new HashMap<>();   // id → {answer_no, points}. 출제 후 삭제된 문제도 채점
        jdbc.query("SELECT id, answer_no, points FROM quiz WHERE id IN (" + ids.stream().map(i -> "?").collect(Collectors.joining(",")) + ")",
                rs -> { key.put(rs.getLong("id"), new int[]{rs.getInt("answer_no"), rs.getInt("points")}); }, ids.toArray());
        int score = 0, max = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (Long id : ids) {
            int[] k = key.get(id);
            if (k == null) continue;
            boolean correct = Objects.equals(picked.get(id), k[0]);
            if (correct) score += k[1];
            max += k[1];
            results.add(Map.of("id", id, "correct", correct));
        }
        int updated = jdbc.update("UPDATE quiz_attempt SET score=?, submitted_at=datetime('now') WHERE id=? AND submitted_at IS NULL", score, aid);
        if (updated == 0) return ResponseEntity.status(409).build();
        return ResponseEntity.ok(Map.of("score", score, "max", max, "results", results));
    }

    private List<String> parse(String choices) {
        try { return json.readValue(choices, STRINGS); } catch (JsonProcessingException e) { return List.of(); }
    }
}
