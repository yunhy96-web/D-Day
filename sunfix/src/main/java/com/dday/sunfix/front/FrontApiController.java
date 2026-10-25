package com.dday.sunfix.front;

import com.dday.sunfix.code.CodeService;
import com.dday.sunfix.code.CodeService.Selection;
import com.dday.sunfix.common.NotFound;
import com.dday.sunfix.common.Times;
import com.dday.sunfix.common.Valid;
import com.dday.sunfix.price.PriceService;
import com.dday.sunfix.price.PriceService.Quote;
import com.dday.sunfix.security.ClientIp;
import com.dday.sunfix.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** 사용자 화면 JSON: 업체 수, 견적 문의, 견적 제보 (설계 §10). 전부 공개. */
@RestController
@RequiredArgsConstructor
public class FrontApiController {
    private final CodeService codes;
    private final PriceService prices;
    private final ReportService reports;
    private final RateLimiter limiter;
    private final JdbcTemplate jdbc;

    public record Inquiry(Long vendorId, String category, List<String> options, String region, String name, String phone, String memo) {}
    public record ReportReq(String category, List<String> options, String region, Long vendorId, String vendorName, Integer price, String ym, String memo) {}

    @GetMapping("/api/count")
    public Map<String, Integer> count(@RequestParam String c, @RequestParam(required = false) String r) {
        return Map.of("count", prices.count(codes.category(c).code(), codes.find(r)));
    }

    /** 총액은 클라이언트 값을 믿지 않고 서버에서 다시 계산해 스냅샷 */
    @PostMapping("/api/inquiry")
    public Map<String, Long> inquiry(@RequestBody Inquiry b, HttpServletRequest req) {
        limiter.check(req, "inquiry", 5, 3600);
        Selection s = codes.select(b.category(), b.options(), b.region());
        PriceService.Result all = prices.compare(s);
        Quote q = Stream.concat(all.open().stream(), all.closed().stream())
                .filter(x -> b.vendorId() != null && x.vendor().id() == b.vendorId()).findFirst().orElseThrow(NotFound::new);
        Long id = jdbc.queryForObject("""
                INSERT INTO inquiry(vendor_id, category_code, option_codes, region_code, quoted_price, name, phone, memo, ip, created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id""", Long.class,
                b.vendorId(), s.category().code(), s.optionKey(), s.region().code(), q.total(),
                Valid.text(b.name(), 30, "name"), Valid.phone(b.phone()), Valid.optText(b.memo(), 500, "memo"), ClientIp.of(req), Times.now());
        return Map.of("id", id);
    }

    @PostMapping("/api/report")
    public Map<String, Long> report(@RequestBody ReportReq b, HttpServletRequest req) {
        limiter.check(req, "report", 3, 86400);
        Selection s = codes.select(b.category(), b.options(), b.region());
        String typed = b.vendorId() == null ? Valid.optText(b.vendorName(), 50, "vendorName") : null;
        long id = reports.submit(s, b.vendorId(), typed, Valid.price(b.price()), Valid.ym(b.ym()),
                Valid.optText(b.memo(), 200, "memo"), ClientIp.of(req));
        return Map.of("id", id);
    }
}
