package com.dday.sunfix.admin;

import com.dday.sunfix.code.CodeService;
import com.dday.sunfix.code.CodeService.Code;
import com.dday.sunfix.common.NotFound;
import com.dday.sunfix.front.ReportService;
import com.dday.sunfix.label.LabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** 관리자 화면 (설계 §8). 저장은 AdminApiController(JSON). */
@Controller
@RequiredArgsConstructor
public class AdminController {
    private final JdbcTemplate jdbc;
    private final CodeService codes;
    private final ReportService reports;
    private final LabelService labels;

    @GetMapping("/admin")
    public String index() { return "redirect:/admin/reports"; }

    @GetMapping("/admin/login")
    public String login() { return "admin/login"; }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("reports", reports.forAdmin());
        return tab(model, "reports");
    }

    @GetMapping("/admin/inquiries")
    public String inquiries(Model model) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT i.*, v.name AS vendor_name FROM inquiry i JOIN vendor v ON v.id = i.vendor_id
                ORDER BY CASE i.status WHEN 'NEW' THEN 0 ELSE 1 END, i.id DESC LIMIT 300""");
        rows.forEach(r -> r.put("options", codes.options((String) r.get("option_codes"))));
        model.addAttribute("rows", rows);
        return tab(model, "inquiries");
    }

    @GetMapping("/admin/vendors")
    public String vendors(Model model) {
        model.addAttribute("rows", jdbc.queryForList("""
                SELECT v.*, (SELECT GROUP_CONCAT(category_code) FROM vendor_category WHERE vendor_id = v.id) AS cats
                FROM vendor v ORDER BY v.id DESC"""));
        return tab(model, "vendors");
    }

    /** id 가 'new' 면 기본 정보 폼만. 저장 후 이 화면으로 와서 지역·카테고리·가격표 */
    @GetMapping("/admin/vendors/{id}")
    public String vendor(@PathVariable String id, Model model) {
        model.addAttribute("regions", codes.regions(true));
        model.addAttribute("categories", codes.children(CodeService.CATEGORY, true));
        if (id.equals("new")) { model.addAttribute("v", Map.of()); return tab(model, "vendors", "admin/vendor"); }
        long vid;
        try { vid = Long.parseLong(id); } catch (NumberFormatException e) { throw new NotFound(); }
        List<Map<String, Object>> v = jdbc.queryForList("SELECT * FROM vendor WHERE id = ?", vid);
        if (v.isEmpty()) throw new NotFound();
        Map<String, Integer> base = new HashMap<>();
        jdbc.query("SELECT category_code, base_price FROM vendor_category WHERE vendor_id = ?",
                rs -> { base.put(rs.getString(1), rs.getObject(2) == null ? null : rs.getInt(2)); }, vid);
        Map<String, Integer> opt = new HashMap<>();
        jdbc.query("SELECT option_code, price FROM vendor_option_price WHERE vendor_id = ?", rs -> { opt.put(rs.getString(1), rs.getInt(2)); }, vid);
        Map<String, List<CodeService.OptionGroup>> groups = new HashMap<>();
        for (String c : base.keySet()) groups.put(c, codes.optionGroups(c, true));
        model.addAttribute("v", v.get(0));
        model.addAttribute("vRegions", new HashSet<>(jdbc.queryForList("SELECT region_code FROM vendor_region WHERE vendor_id = ?", String.class, vid)));
        model.addAttribute("base", base);
        model.addAttribute("opt", opt);
        model.addAttribute("groups", groups);
        return tab(model, "vendors", "admin/vendor");
    }

    @GetMapping("/admin/codes")
    public String codes(@RequestParam(defaultValue = CodeService.CATEGORY) String root, Model model) {
        String r = CodeService.REGION.equals(root) ? CodeService.REGION : CodeService.CATEGORY;
        model.addAttribute("root", r);
        model.addAttribute("tree", tree(r));
        model.addAttribute("ko", labels.raw("ko"));
        model.addAttribute("en", labels.raw("en"));
        return tab(model, "codes");
    }

    @GetMapping("/admin/labels")
    public String labels(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "false") boolean missing, Model model) {
        model.addAttribute("rows", jdbc.queryForList("""
                SELECT key, MAX(CASE lang WHEN 'ko' THEN text END) AS ko, MAX(CASE lang WHEN 'en' THEN text END) AS en
                FROM label GROUP BY key
                HAVING (? = '' OR key LIKE '%' || ? || '%' OR ko LIKE '%' || ? || '%' OR en LIKE '%' || ? || '%') AND (? = 0 OR en IS NULL)
                ORDER BY key LIMIT 500""", q, q, q, q, missing ? 1 : 0));
        model.addAttribute("q", q);
        model.addAttribute("missing", missing);
        return tab(model, "labels");
    }

    /** 트리 한 줄: 코드 + 깊이 (화면은 들여쓰기만) */
    public record Node(Code code, int depth) {}

    private List<Node> tree(String root) {
        List<Node> out = new ArrayList<>();
        walk(root, 0, out);
        return out;
    }

    private void walk(String parent, int depth, List<Node> out) {
        for (Code c : codes.children(parent, false)) { out.add(new Node(c, depth)); walk(c.code(), depth + 1, out); }
    }

    private static String tab(Model model, String tab) { return tab(model, tab, "admin/" + tab); }

    private static String tab(Model model, String tab, String view) { model.addAttribute("tab", tab); return view; }
}
