package com.dday.sunfix.admin;

import com.dday.sunfix.code.CodeService;
import com.dday.sunfix.common.BadRequest;
import com.dday.sunfix.common.NotFound;
import com.dday.sunfix.common.Times;
import com.dday.sunfix.common.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 영업 리드 (설계 §8-1). 가격을 공개한 실제 업체에 연락 → 동의하면 업체로 등록. */
@Controller
@RequiredArgsConstructor
public class LeadController {
    public static final Set<String> STATUSES = Set.of("TODO", "CONTACTED", "AGREED", "DECLINED");
    private static final Pattern PHONE = Pattern.compile("0\\d{1,3}-\\d{3,4}-\\d{4}|1\\d{3}-\\d{4}");
    private final JdbcTemplate jdbc;
    private final CodeService codes;

    public record LeadReq(String category, String name, String region, String channel, String prices, String conditions,
                          String contact, String sourceUrl, String sourceDate, Integer priority, String reason, String checkedAt) {}
    public record LeadUpdate(String status, String memo, Integer priority) {}

    @GetMapping("/admin/leads")
    public String list(@RequestParam(required = false) String c, @RequestParam(required = false) String status, Model model) {
        List<Object> args = new ArrayList<>();
        String where = " WHERE 1 = 1";
        if (c != null && !c.isBlank()) { where += " AND l.category_code = ?"; args.add(c); }
        if (status != null && STATUSES.contains(status)) { where += " AND l.status = ?"; args.add(status); }
        model.addAttribute("rows", jdbc.queryForList("""
                SELECT l.*, v.name AS vendor_name FROM sales_lead l LEFT JOIN vendor v ON v.id = l.vendor_id""" + where
                + " ORDER BY CASE l.status WHEN 'DECLINED' THEN 1 ELSE 0 END, l.priority, l.category_code, l.name", args.toArray()));
        model.addAttribute("cats", jdbc.queryForList("SELECT category_code, COUNT(*) AS n FROM sales_lead GROUP BY category_code ORDER BY category_code"));
        model.addAttribute("statusCounts", jdbc.queryForList("SELECT status, COUNT(*) AS n FROM sales_lead GROUP BY status"));
        model.addAttribute("c", c);
        model.addAttribute("status", status);
        model.addAttribute("tab", "leads");
        return "admin/leads";
    }

    /** scripts/import-leads.sh 가 부른다. (카테고리, 상호) 가 같으면 조사 칸만 갱신하고 상태·메모·연결은 그대로 */
    @Transactional
    @PostMapping("/admin/api/leads/import")
    @ResponseBody
    public Map<String, Integer> importLeads(@RequestBody List<LeadReq> leads) {
        for (LeadReq b : leads) {
            codes.leaf(b.category());
            int priority = b.priority() == null ? 2 : b.priority();
            if (priority < 1 || priority > 3) throw new BadRequest("priority");
            jdbc.update("""
                    INSERT INTO sales_lead(category_code, name, region, channel, prices, conditions, contact, source_url, source_date, priority, reason, checked_at, updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT(category_code, name) DO UPDATE SET region = excluded.region, channel = excluded.channel, prices = excluded.prices,
                      conditions = excluded.conditions, contact = excluded.contact, source_url = excluded.source_url, source_date = excluded.source_date,
                      priority = excluded.priority, reason = excluded.reason, checked_at = excluded.checked_at, updated_at = excluded.updated_at""",
                    b.category(), Valid.text(b.name(), 80, "name"), Valid.optText(b.region(), 200, "text"), Valid.optText(b.channel(), 50, "text"),
                    Valid.optText(b.prices(), 2000, "text"), Valid.optText(b.conditions(), 2000, "text"), Valid.optText(b.contact(), 300, "text"),
                    Valid.optText(b.sourceUrl(), 1000, "text"), Valid.optText(b.sourceDate(), 50, "text"), priority, Valid.optText(b.reason(), 1000, "text"),
                    Valid.text(b.checkedAt(), 20, "text"), Times.now());
        }
        return Map.of("count", leads.size());
    }

    /** 상태·메모·우선순위 중 온 것만 (칸 단위 자동 저장) */
    @PutMapping("/admin/api/leads/{id}")
    @ResponseBody
    public ResponseEntity<Void> update(@PathVariable long id, @RequestBody LeadUpdate b) {
        exists(id);
        if (b.status() != null) jdbc.update("UPDATE sales_lead SET status = ?, updated_at = ? WHERE id = ?", Valid.oneOf(b.status(), STATUSES, "status"), Times.now(), id);
        if (b.memo() != null) jdbc.update("UPDATE sales_lead SET memo = ?, updated_at = ? WHERE id = ?", Valid.optText(b.memo(), 2000, "memo"), Times.now(), id);
        if (b.priority() != null) {
            if (b.priority() < 1 || b.priority() > 3) throw new BadRequest("priority");
            jdbc.update("UPDATE sales_lead SET priority = ?, updated_at = ? WHERE id = ?", b.priority(), Times.now(), id);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * 입점 동의 → 업체 등록. 가격을 넣기 전까지는 사용자 화면에 안 보이게 use_yn='N'.
     * 공개 연락처에서 첫 전화번호, 카테고리 연결까지 해 두고 업체 편집 화면으로 보낸다.
     */
    @Transactional
    @PostMapping("/admin/api/leads/{id}/vendor")
    @ResponseBody
    public Map<String, Long> toVendor(@PathVariable long id) {
        Map<String, Object> l = exists(id);
        if (l.get("vendor_id") != null) return Map.of("id", ((Number) l.get("vendor_id")).longValue());
        Matcher m = PHONE.matcher(l.get("contact") == null ? "" : (String) l.get("contact"));
        Long vid = jdbc.queryForObject("""
                INSERT INTO vendor(name, phone, address, intro, sample_yn, use_yn, created_at) VALUES (?,?,?,?, 'N', 'N', ?) RETURNING id""", Long.class,
                l.get("name"), m.find() ? m.group() : null, l.get("region") == null ? "-" : l.get("region"), null, Times.now());
        jdbc.update("INSERT OR IGNORE INTO vendor_category(vendor_id, category_code) VALUES (?,?)", vid, l.get("category_code"));
        jdbc.update("UPDATE sales_lead SET vendor_id = ?, status = 'AGREED', updated_at = ? WHERE id = ?", vid, Times.now(), id);
        return Map.of("id", vid);
    }

    private Map<String, Object> exists(long id) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT * FROM sales_lead WHERE id = ?", id);
        if (l.isEmpty()) throw new NotFound();
        return l.get(0);
    }
}
