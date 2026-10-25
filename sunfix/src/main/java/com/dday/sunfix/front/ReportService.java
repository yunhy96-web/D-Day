package com.dday.sunfix.front;

import com.dday.sunfix.code.CodeService;
import com.dday.sunfix.code.CodeService.Code;
import com.dday.sunfix.code.CodeService.Selection;
import com.dday.sunfix.common.BadRequest;
import com.dday.sunfix.common.Times;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 견적 제보 (설계 §7). 비회원 → PENDING → 관리자 승인 후 공개. 직접 입력한 업체명은 공개하지 않는다. */
@Service
@RequiredArgsConstructor
public class ReportService {
    public static final int MEDIAN_MIN = 3;
    private final JdbcTemplate jdbc;
    private final CodeService codes;

    /** vendorName 은 등록 업체일 때만(공개용). typedName 은 직접 입력(관리자만 본다) */
    public record Report(long id, String categoryCode, List<Code> options, String optionKey, String regionCode,
                         Long vendorId, String vendorName, String typedName, int price, String ym, String memo,
                         String status, String createdAt, boolean outlier) {}

    public long submit(Selection s, Long vendorId, String typedName, int price, String ym, String memo, String ip) {
        if (vendorId != null && jdbc.queryForObject("SELECT COUNT(*) FROM vendor_category WHERE vendor_id = ? AND category_code = ?",
                Integer.class, vendorId, s.category().code()) == 0) throw new BadRequest("vendor");
        return jdbc.queryForObject("""
                INSERT INTO quote_report(category_code, option_codes, region_code, vendor_id, vendor_name, price, quoted_ym, memo, ip, created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id""", Long.class,
                s.category().code(), s.optionKey(), s.region().code(), vendorId, vendorId == null ? typedName : null, price, ym, memo, ip, Times.now());
    }

    /** 결과 화면: 같은 카테고리·같은 시/도의 승인된 제보, 최신 20건 */
    public List<Report> forResult(Selection s) {
        return query("""
                WHERE q.status = 'APPROVED' AND q.category_code = ?
                  AND q.region_code IN (SELECT code FROM common_code WHERE parent_code = ?)
                ORDER BY q.quoted_ym DESC, q.id DESC LIMIT 20""", s.category().code(), s.sido());
    }

    /** 홈: 최근 승인 제보 */
    public List<Report> recent(int n) {
        return query("WHERE q.status = 'APPROVED' ORDER BY q.id DESC LIMIT ?", n);
    }

    /** 같은 옵션 조합(전 지역) 승인 제보가 MEDIAN_MIN 건 이상이면 중간값 */
    public Integer median(String category, String optionKey) {
        return median(jdbc.queryForList("SELECT price FROM quote_report WHERE status = 'APPROVED' AND category_code = ? AND option_codes = ?",
                Integer.class, category, optionKey));
    }

    public int sameCount(String category, String optionKey) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM quote_report WHERE status = 'APPROVED' AND category_code = ? AND option_codes = ?",
                Integer.class, category, optionKey);
    }

    /** 관리자: 대기 먼저, 최신순. 같은 조합 승인 중간값의 1/3 미만·3배 초과면 outlier */
    public List<Report> forAdmin() {
        return query("ORDER BY CASE q.status WHEN 'PENDING' THEN 0 ELSE 1 END, q.id DESC LIMIT 300");
    }

    public void status(long id, String status) {
        if (jdbc.update("UPDATE quote_report SET status = ? WHERE id = ?", status, id) == 0) throw new BadRequest("notFound");
    }

    private List<Report> query(String tail, Object... args) {
        List<Object[]> rows = jdbc.query("""
                SELECT q.id, q.category_code, q.option_codes, q.region_code, q.vendor_id, v.name, q.vendor_name,
                       q.price, q.quoted_ym, q.memo, q.status, q.created_at
                FROM quote_report q LEFT JOIN vendor v ON v.id = q.vendor_id
                """ + tail, (rs, i) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getObject(5) == null ? null : rs.getLong(5), rs.getString(6), rs.getString(7), rs.getInt(8),
                rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12)}, args);
        Map<String, Integer> medians = new HashMap<>();
        List<Report> out = new ArrayList<>();
        for (Object[] r : rows) {
            String cat = (String) r[1], key = (String) r[2];
            Integer med = medians.computeIfAbsent(cat + "|" + key, k -> median(cat, key));
            int price = (int) r[7];
            boolean outlier = med != null && (price * 3 < med || price > med * 3);
            out.add(new Report((long) r[0], cat, codes.options(key), key, (String) r[3], (Long) r[4], (String) r[5], (String) r[6],
                    price, (String) r[8], (String) r[9], (String) r[10], (String) r[11], outlier));
        }
        return out;
    }

    static Integer median(List<Integer> prices) {
        if (prices.size() < MEDIAN_MIN) return null;
        List<Integer> p = prices.stream().sorted().toList();
        int n = p.size();
        return n % 2 == 1 ? p.get(n / 2) : (p.get(n / 2 - 1) + p.get(n / 2)) / 2;
    }
}
