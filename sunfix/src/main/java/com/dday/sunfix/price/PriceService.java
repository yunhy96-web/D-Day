package com.dday.sunfix.price;

import com.dday.sunfix.code.CodeService.Code;
import com.dday.sunfix.code.CodeService.Selection;
import com.dday.sunfix.common.Rs;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 가격 비교 (설계 §5). 총액 = 기본가 + 고른 옵션 추가금 합.
 * 기본가나 옵션 가격이 하나라도 없으면 '미공개'.
 */
@Service
@RequiredArgsConstructor
public class PriceService {
    private final JdbcTemplate jdbc;

    /** 조건 칸(vat·tripFee·failFee·surcharge·extraNote)은 null = 업체가 밝히지 않음 (설계 §5-1) */
    public record Vendor(long id, String name, String nameEn, String phone, String address, boolean sample,
                         String vat, Integer tripFee, Integer failFee, String surcharge, String extraNote) {
        public String name(String lang) { return "en".equals(lang) && nameEn != null ? nameEn : name; }
    }
    /** price 가 null 이면 미공개 */
    public record Line(Code option, Integer price) {}
    public record Quote(Vendor vendor, Integer basePrice, List<Line> lines, Integer total, int reportCount, Integer reportAvg) {
        public boolean open() { return total != null; }
    }
    public record Result(List<Quote> open, List<Quote> closed, Integer min, Integer avg, Integer max) {}

    public Result compare(Selection s) {
        List<Object[]> rows = jdbc.query("""
                SELECT v.id, v.name, v.name_en, v.phone, v.address, v.sample_yn, vc.base_price,
                       v.vat, v.trip_fee, v.fail_fee, v.surcharge, v.extra_note
                FROM vendor v JOIN vendor_category vc ON vc.vendor_id = v.id AND vc.category_code = ?
                WHERE v.use_yn = 'Y'
                  AND EXISTS (SELECT 1 FROM vendor_region r WHERE r.vendor_id = v.id AND r.region_code IN (?, ?))""",
                (rs, i) -> new Object[]{new Vendor(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), "Y".equals(rs.getString(6)),
                        rs.getString(8), Rs.integer(rs, 9), Rs.integer(rs, 10), rs.getString(11), rs.getString(12)),
                        rs.getObject(7) == null ? null : rs.getInt(7)},
                s.category().code(), s.region().code(), s.sido());

        List<String> optCodes = s.options().stream().map(Code::code).toList();
        String in = String.join(",", Collections.nCopies(optCodes.size(), "?"));
        Map<String, Integer> prices = new HashMap<>();   // "vendorId:option" → price
        jdbc.query("SELECT vendor_id, option_code, price FROM vendor_option_price WHERE option_code IN (" + in + ")",
                (RowCallbackHandler) rs -> prices.put(rs.getLong(1) + ":" + rs.getString(2), rs.getInt(3)), optCodes.toArray());
        Map<Long, int[]> reports = new HashMap<>();       // vendorId → [건수, 평균]
        jdbc.query("""
                SELECT vendor_id, COUNT(*), CAST(AVG(price) AS INTEGER) FROM quote_report
                WHERE status = 'APPROVED' AND category_code = ? AND option_codes = ? AND vendor_id IS NOT NULL GROUP BY vendor_id""",
                (RowCallbackHandler) rs -> reports.put(rs.getLong(1), new int[]{rs.getInt(2), rs.getInt(3)}), s.category().code(), s.optionKey());

        List<Quote> open = new ArrayList<>(), closed = new ArrayList<>();
        for (Object[] r : rows) {
            Vendor v = (Vendor) r[0];
            Integer base = (Integer) r[1];
            List<Line> lines = s.options().stream().map(o -> new Line(o, prices.get(v.id() + ":" + o.code()))).toList();
            Integer total = base == null || lines.stream().anyMatch(l -> l.price() == null) ? null
                    : base + lines.stream().mapToInt(Line::price).sum();
            int[] rep = reports.get(v.id());
            Quote q = new Quote(v, base, lines, total, rep == null ? 0 : rep[0], rep == null ? null : rep[1]);
            (q.open() ? open : closed).add(q);
        }
        open.sort(Comparator.comparingInt(Quote::total));
        closed.sort(Comparator.comparing(q -> q.vendor().name()));
        if (open.isEmpty()) return new Result(open, closed, null, null, null);
        int avg = (int) Math.round(open.stream().mapToInt(Quote::total).average().orElse(0) / 1000) * 1000;
        return new Result(open, closed, open.get(0).total(), avg, open.get(open.size() - 1).total());
    }

    /** 리프 카테고리들(+지역)을 다루는 업체 수. 옵션을 다 고르기 전에도 보여주려고 가격은 안 본다 */
    public int count(List<Code> leaves, Code region) {
        if (leaves.isEmpty()) return 0;
        List<Object> args = new ArrayList<>(leaves.stream().map(Code::code).toList());
        String sql = "SELECT COUNT(DISTINCT v.id) FROM vendor_category vc JOIN vendor v ON v.id = vc.vendor_id WHERE v.use_yn = 'Y' AND vc.category_code IN ("
                + String.join(",", Collections.nCopies(leaves.size(), "?")) + ")";
        if (region != null) {
            sql += " AND EXISTS (SELECT 1 FROM vendor_region r WHERE r.vendor_id = v.id AND r.region_code IN (?, ?))";
            args.add(region.code());
            args.add(region.parent() == null ? region.code() : region.parent());
        }
        return jdbc.queryForObject(sql, Integer.class, args.toArray());
    }
}
