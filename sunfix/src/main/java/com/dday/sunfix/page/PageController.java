package com.dday.sunfix.page;

import com.dday.sunfix.code.CodeService;
import com.dday.sunfix.code.CodeService.Code;
import com.dday.sunfix.code.CodeService.Selection;
import com.dday.sunfix.common.BadRequest;
import com.dday.sunfix.common.NotFound;
import com.dday.sunfix.front.ReportService;
import com.dday.sunfix.price.PriceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper json;

    @GetMapping("/")
    public String home(@RequestParam(required = false) String sido, Model model) {
        List<Code> cats = codes.subCategories(CodeService.CATEGORY, true);
        model.addAttribute("categories", cats);
        model.addAttribute("counts", counts(cats));
        model.addAttribute("sidos", codes.children(CodeService.REGION, true));
        model.addAttribute("sido", codes.find(sido) != null ? sido : null);
        model.addAttribute("leafCount", codes.leaves(CodeService.CATEGORY, true).size());
        model.addAttribute("vendorCount", jdbc.queryForObject("SELECT COUNT(*) FROM vendor WHERE use_yn = 'Y'", Integer.class));
        model.addAttribute("reportCount", jdbc.queryForObject("SELECT COUNT(*) FROM quote_report WHERE status = 'APPROVED'", Integer.class));
        model.addAttribute("recent", reports.recent(5));
        searchModel(model);
        exampleModel(model);
        return "home";
    }

    /**
     * 랜딩 "가격이 먼저 보여요" 예시: 자주 찾는 수리(없으면 첫 리프)의 각 옵션 그룹 첫 옵션 + 첫 시/군/구로 실제 비교 결과.
     * 관리자가 코드를 바꿔 선택이 안 되면 예시만 숨긴다.
     */
    @SuppressWarnings("unchecked")
    private void exampleModel(Model model) {
        List<Code> popular = (List<Code>) model.getAttribute("popular");
        List<Code> leaves = codes.leaves(CodeService.CATEGORY, true);
        Code leaf = !popular.isEmpty() ? popular.get(0) : leaves.isEmpty() ? null : leaves.get(0);
        List<CodeService.Region> regions = codes.regions(true);
        if (leaf == null || regions.isEmpty() || regions.get(0).subs().isEmpty()) return;
        try {
            Selection s = codes.select(leaf.code(), codes.optionGroups(leaf.code(), true).stream().map(g -> g.options().get(0).code()).toList(),
                    regions.get(0).subs().get(0).code());
            model.addAttribute("ex", s);
            model.addAttribute("exRes", prices.compare(s));
            model.addAttribute("exQuery", query(s));
        } catch (BadRequest | NotFound e) { /* 예시 생략 */ }
    }

    /**
     * 홈 검색(설계 §3-1): 리프 카테고리 수십 개라 서버 검색 없이 페이지에 목록·검색어를 넣고 브라우저에서 거른다.
     * ponytail: 리프가 수백 개를 넘으면 /api/search 로
     */
    private void searchModel(Model model) {
        Fmt f = (Fmt) model.getAttribute("f");
        List<Map<String, String>> index = codes.leaves(CodeService.CATEGORY, true).stream().map(c -> Map.of(
                "code", c.code(), "icon", c.icon() == null ? "🔧" : c.icon(), "path", f.path(c.code()),
                "kw", f.labels().getOrDefault("kw." + c.code(), ""))).toList();
        try { model.addAttribute("searchIndex", json.writeValueAsString(index).replace("<", "\\u003c")); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
        // 자주 찾는 수리: 승인 제보가 많은 리프
        model.addAttribute("popular", jdbc.queryForList("""
                SELECT category_code FROM quote_report WHERE status = 'APPROVED' GROUP BY category_code ORDER BY COUNT(*) DESC LIMIT 8""", String.class)
                .stream().map(codes::find).filter(c -> c != null && codes.isLeaf(c.code()) && codes.path(c.code()).stream().allMatch(Code::use)).limit(4).toList());
    }

    /** 하위 카테고리가 있으면 고르는 격자, 리프면 옵션 선택 */
    @GetMapping("/c/{category}")
    public String category(@PathVariable String category, @RequestParam(required = false) String sido,
                           @RequestParam(required = false) List<String> o, @RequestParam(required = false) String r, Model model) {
        Code cat = codes.category(category);
        model.addAttribute("path", codes.path(cat.code()));
        if (!codes.isLeaf(cat.code())) {
            List<Code> subs = codes.subCategories(cat.code(), true);
            model.addAttribute("cat", cat);
            model.addAttribute("subs", subs);
            model.addAttribute("counts", counts(subs));
            model.addAttribute("sido", codes.find(sido) != null ? sido : null);
            return "categories";
        }
        pickModel(model, cat, sido, o, r);
        return "options";
    }

    @GetMapping("/c/{category}/result")
    public String result(@PathVariable String category, @RequestParam(required = false) List<String> o,
                         @RequestParam(required = false) String r, @RequestParam(defaultValue = "asc") String sort, Model model) {
        Selection s;
        try { s = codes.select(category, o, r); }
        catch (BadRequest e) { return "redirect:/c/" + codes.category(category).code(); }   // 덜 고른 URL·리프 아닌 카테고리는 고르는 화면으로
        PriceService.Result res = prices.compare(s);
        if ("desc".equals(sort)) Collections.reverse(res.open());   // open 은 compare 가 만든 ArrayList
        model.addAttribute("s", s);
        model.addAttribute("path", codes.path(s.category().code()));
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
        Code cat = c == null || c.isBlank() ? null : codes.category(c);
        if (cat == null || !codes.isLeaf(cat.code())) {   // 리프까지 내려가며 고르기
            model.addAttribute("parent", cat);
            model.addAttribute("categories", codes.subCategories(cat == null ? CodeService.CATEGORY : cat.code(), true));
            return "report";
        }
        pickModel(model, cat, null, o, r);
        model.addAttribute("vendors", jdbc.queryForList("""
                SELECT v.id, v.name, v.name_en FROM vendor v JOIN vendor_category vc ON vc.vendor_id = v.id
                WHERE v.use_yn = 'Y' AND vc.category_code = ? ORDER BY v.name""", cat.code()));
        return "report";
    }

    private Map<String, Integer> counts(List<Code> cats) {
        Map<String, Integer> counts = new HashMap<>();
        for (Code c : cats) counts.put(c.code(), prices.count(codes.leaves(c.code(), true), null));
        return counts;
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
