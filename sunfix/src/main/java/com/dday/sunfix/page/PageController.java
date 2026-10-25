package com.dday.sunfix.page;

import com.dday.sunfix.code.CodeService;
import com.dday.sunfix.code.CodeService.Code;
import com.dday.sunfix.code.CodeService.Selection;
import com.dday.sunfix.common.BadRequest;
import com.dday.sunfix.common.NotFound;
import com.dday.sunfix.front.ReportService;
import com.dday.sunfix.price.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 사용자 화면 (설계 §3). 결과 URL 은 GET 파라미터만으로 재현·공유된다. */
@Controller
@RequiredArgsConstructor
public class PageController {
    private final CodeService codes;
    private final PriceService prices;
    private final ReportService reports;
    private final JdbcTemplate jdbc;

    @GetMapping("/")
    public String home(@RequestParam(required = false) String sido, Model model) {
        List<Code> cats = codes.children(CodeService.CATEGORY, true);
        Map<String, Integer> counts = new HashMap<>();
        for (Code c : cats) counts.put(c.code(), prices.count(c.code(), null));
        model.addAttribute("categories", cats);
        model.addAttribute("counts", counts);
        model.addAttribute("sidos", codes.children(CodeService.REGION, true));
        model.addAttribute("sido", codes.find(sido) != null ? sido : null);
        model.addAttribute("vendorCount", jdbc.queryForObject("SELECT COUNT(*) FROM vendor WHERE use_yn = 'Y'", Integer.class));
        model.addAttribute("reportCount", jdbc.queryForObject("SELECT COUNT(*) FROM quote_report WHERE status = 'APPROVED'", Integer.class));
        model.addAttribute("recent", reports.recent(5));
        return "home";
    }

    @GetMapping("/c/{category}")
    public String options(@PathVariable String category, @RequestParam(required = false) String sido,
                          @RequestParam(required = false) List<String> o, @RequestParam(required = false) String r, Model model) {
        pickModel(model, codes.category(category), sido, o, r);
        return "options";
    }

    @GetMapping("/c/{category}/result")
    public String result(@PathVariable String category, @RequestParam(required = false) List<String> o,
                         @RequestParam(required = false) String r, @RequestParam(defaultValue = "asc") String sort, Model model) {
        Selection s;
        try { s = codes.select(category, o, r); }
        catch (BadRequest e) { return "redirect:/c/" + codes.category(category).code(); }   // 덜 고른 URL 은 고르는 화면으로
        PriceService.Result res = prices.compare(s);
        if ("desc".equals(sort)) Collections.reverse(res.open());   // open 은 compare 가 만든 ArrayList
        model.addAttribute("s", s);
        model.addAttribute("res", res);
        model.addAttribute("sort", "desc".equals(sort) ? "desc" : "asc");
        model.addAttribute("query", query(s));
        model.addAttribute("reports", reports.forResult(s));
        model.addAttribute("median", reports.median(category, s.optionKey()));
        model.addAttribute("sameCount", reports.sameCount(category, s.optionKey()));
        return "result";
    }

    @GetMapping("/v/{id}")
    public String vendor(@PathVariable long id, Model model) {
        List<Map<String, Object>> v = jdbc.queryForList("SELECT * FROM vendor WHERE id = ? AND use_yn = 'Y'", id);
        if (v.isEmpty()) throw new NotFound();
        Map<String, Integer> optPrices = new HashMap<>();
        jdbc.query("SELECT option_code, price FROM vendor_option_price WHERE vendor_id = ?", rs -> { optPrices.put(rs.getString(1), rs.getInt(2)); }, id);
        Map<String, Integer> basePrices = new HashMap<>();
        List<Code> cats = jdbc.query("SELECT category_code, base_price FROM vendor_category WHERE vendor_id = ?", (rs, i) -> {
            basePrices.put(rs.getString(1), rs.getObject(2) == null ? null : rs.getInt(2));
            return codes.find(rs.getString(1));
        }, id).stream().filter(c -> c != null && c.use()).sorted(Comparator.comparingInt(Code::sort)).toList();
        model.addAttribute("v", v.get(0));
        model.addAttribute("cats", cats);
        model.addAttribute("groups", cats.stream().collect(Collectors.toMap(Code::code, c -> codes.optionGroups(c.code(), true))));
        model.addAttribute("basePrices", basePrices);
        model.addAttribute("optPrices", optPrices);
        model.addAttribute("regions", jdbc.queryForList("SELECT region_code FROM vendor_region WHERE vendor_id = ?", String.class, id));
        return "vendor";
    }

    @GetMapping("/report")
    public String report(@RequestParam(required = false) String c, @RequestParam(required = false) List<String> o,
                         @RequestParam(required = false) String r, Model model) {
        if (c == null) { model.addAttribute("categories", codes.children(CodeService.CATEGORY, true)); return "report"; }
        Code cat = codes.category(c);
        pickModel(model, cat, null, o, r);
        model.addAttribute("vendors", jdbc.queryForList("""
                SELECT v.id, v.name, v.name_en FROM vendor v JOIN vendor_category vc ON vc.vendor_id = v.id
                WHERE v.use_yn = 'Y' AND vc.category_code = ? ORDER BY v.name""", cat.code()));
        return "report";
    }

    /** 고르는 화면 공통(옵션 선택·제보). o·r·sido 로 미리 선택 */
    private void pickModel(Model model, Code cat, String sido, List<String> o, String r) {
        Code reg = codes.find(r);
        model.addAttribute("cat", cat);
        model.addAttribute("groups", codes.optionGroups(cat.code(), true));
        model.addAttribute("regions", codes.regions(true));
        model.addAttribute("picked", o == null ? List.of() : o);
        model.addAttribute("pickedRegion", reg != null && CodeService.REGION_SUB.equals(reg.group()) ? reg.code() : null);
        model.addAttribute("pickedSido", reg != null && reg.parent() != null ? reg.parent() : codes.find(sido) != null ? sido : null);
    }

    private static String query(Selection s) {
        StringBuilder q = new StringBuilder();
        for (Code o : s.options()) q.append("o=").append(o.code()).append('&');
        return q.append("r=").append(s.region().code()).toString();
    }
}
