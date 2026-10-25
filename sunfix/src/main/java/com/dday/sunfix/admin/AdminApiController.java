package com.dday.sunfix.admin;

import com.dday.sunfix.code.CodeService;
import com.dday.sunfix.code.CodeService.Code;
import com.dday.sunfix.common.BadRequest;
import com.dday.sunfix.common.NotFound;
import com.dday.sunfix.common.Times;
import com.dday.sunfix.common.Valid;
import com.dday.sunfix.front.ReportService;
import com.dday.sunfix.label.LabelService;
import com.dday.sunfix.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 관리자 저장 API (설계 §8). 가격표·코드·라벨은 칸 단위 자동 저장. */
@RestController
@RequiredArgsConstructor
public class AdminApiController {
    private static final Set<String> YN = Set.of("Y", "N");
    private final AdminAuth auth;
    private final RateLimiter limiter;
    private final JdbcTemplate jdbc;
    private final CodeService codes;
    private final LabelService labels;
    private final ReportService reports;

    public record Login(String loginId, String password) {}
    public record Status(String status) {}
    public record VendorReq(String name, String nameEn, String phone, String address, String intro, String sampleYn, String useYn) {}
    public record Codes(List<String> codes) {}
    public record PriceReq(String categoryCode, String optionCode, Integer price) {}
    public record NewCode(String code, String parentCode, String rootGroup, String ko, String en) {}
    public record CodeReq(Integer sort, String useYn, String ko, String en) {}
    public record LabelReq(String key, String lang, String text) {}

    @PostMapping("/admin/api/login")
    public ResponseEntity<Void> login(@RequestBody Login b, HttpServletRequest req, HttpServletResponse res) {
        limiter.check(req, "admin-login", 10, 600);
        if (!auth.login(b.loginId() == null ? "" : b.loginId().strip(), b.password(), res)) throw new BadRequest("loginFail");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/api/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        auth.logout(req, res);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/api/reports/{id}/status")
    public ResponseEntity<Void> reportStatus(@PathVariable long id, @RequestBody Status b) {
        reports.status(id, Valid.oneOf(b.status(), Set.of("PENDING", "APPROVED", "REJECTED"), "status"));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/api/inquiries/{id}/status")
    public ResponseEntity<Void> inquiryStatus(@PathVariable long id, @RequestBody Status b) {
        if (jdbc.update("UPDATE inquiry SET status = ? WHERE id = ?", Valid.oneOf(b.status(), Set.of("NEW", "DONE"), "status"), id) == 0) throw new NotFound();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/api/vendors")
    public Map<String, Long> createVendor(@RequestBody VendorReq b) {
        Long id = jdbc.queryForObject("""
                INSERT INTO vendor(name, name_en, phone, address, intro, sample_yn, use_yn, created_at)
                VALUES (?,?,?,?,?,?,?,?) RETURNING id""", Long.class, vendorArgs(b, Times.now()));
        return Map.of("id", id);
    }

    @PutMapping("/admin/api/vendors/{id}")
    public ResponseEntity<Void> updateVendor(@PathVariable long id, @RequestBody VendorReq b) {
        Object[] a = vendorArgs(b, null);
        if (jdbc.update("UPDATE vendor SET name=?, name_en=?, phone=?, address=?, intro=?, sample_yn=?, use_yn=? WHERE id=?",
                a[0], a[1], a[2], a[3], a[4], a[5], a[6], id) == 0) throw new NotFound();
        return ResponseEntity.noContent().build();
    }

    /** 출장 지역 통째 교체. 시/도(전체) 또는 시/군/구 */
    @Transactional
    @PutMapping("/admin/api/vendors/{id}/regions")
    public ResponseEntity<Void> regions(@PathVariable long id, @RequestBody Codes b) {
        vendorExists(id);
        List<String> list = b.codes() == null ? List.of() : b.codes();
        for (String c : list) {
            Code code = codes.find(c);
            if (code == null || !Set.of(CodeService.REGION, CodeService.REGION_SUB).contains(code.group())) throw new BadRequest("region");
        }
        jdbc.update("DELETE FROM vendor_region WHERE vendor_id = ?", id);
        for (String c : list) jdbc.update("INSERT OR IGNORE INTO vendor_region(vendor_id, region_code) VALUES (?,?)", id, c);
        return ResponseEntity.noContent().build();
    }

    /** 다루는 카테고리 교체. 남는 카테고리의 기본가는 유지, 빠진 카테고리 행만 지운다(옵션 가격은 남겨 둠) */
    @Transactional
    @PutMapping("/admin/api/vendors/{id}/categories")
    public ResponseEntity<Void> categories(@PathVariable long id, @RequestBody Codes b) {
        vendorExists(id);
        List<String> list = b.codes() == null ? List.of() : b.codes();
        for (String c : list) {
            Code code = codes.find(c);
            if (code == null || !CodeService.CATEGORY.equals(code.group())) throw new BadRequest("category");
        }
        jdbc.query("SELECT category_code FROM vendor_category WHERE vendor_id = ?", (rs, i) -> rs.getString(1), id).stream()
                .filter(c -> !list.contains(c)).forEach(c -> jdbc.update("DELETE FROM vendor_category WHERE vendor_id = ? AND category_code = ?", id, c));
        for (String c : list) jdbc.update("INSERT OR IGNORE INTO vendor_category(vendor_id, category_code) VALUES (?,?)", id, c);
        return ResponseEntity.noContent().build();
    }

    /** 가격 한 칸. optionCode 가 없으면 기본가. price null = 미공개 */
    @PutMapping("/admin/api/vendors/{id}/prices")
    public ResponseEntity<Void> price(@PathVariable long id, @RequestBody PriceReq b) {
        Integer price = Valid.optPrice(b.price());
        if (b.optionCode() == null) {
            if (jdbc.update("UPDATE vendor_category SET base_price = ? WHERE vendor_id = ? AND category_code = ?", price, id, b.categoryCode()) == 0)
                throw new BadRequest("category");
            return ResponseEntity.noContent().build();
        }
        Code opt = codes.find(b.optionCode());
        if (opt == null || !CodeService.OPT.equals(opt.group())) throw new BadRequest("option");
        vendorExists(id);
        if (price == null) jdbc.update("DELETE FROM vendor_option_price WHERE vendor_id = ? AND option_code = ?", id, opt.code());
        else jdbc.update("""
                INSERT INTO vendor_option_price(vendor_id, option_code, price, updated_at) VALUES (?,?,?,?)
                ON CONFLICT(vendor_id, option_code) DO UPDATE SET price = excluded.price, updated_at = excluded.updated_at""",
                id, opt.code(), price, Times.now());
        return ResponseEntity.noContent().build();
    }

    /** 코드 추가. 부모가 있으면 부모 그룹의 자식 그룹, 없으면 CATEGORY·REGION 루트 */
    @Transactional
    @PostMapping("/admin/api/codes")
    public ResponseEntity<Void> createCode(@RequestBody NewCode b) {
        String code = Valid.code(b.code() == null ? null : b.code().strip().toUpperCase());
        if (codes.find(code) != null) throw new BadRequest("codeTaken");
        String group;
        if (b.parentCode() != null) {
            Code parent = codes.find(b.parentCode());
            if (parent == null || !CodeService.CHILD_GROUP.containsKey(parent.group())) throw new BadRequest("parent");
            group = CodeService.CHILD_GROUP.get(parent.group());
        } else group = Valid.oneOf(b.rootGroup(), Set.of(CodeService.CATEGORY, CodeService.REGION), "group");
        int sort = jdbc.queryForObject("SELECT COALESCE(MAX(sort_no), 0) + 1 FROM common_code WHERE group_code = ? AND parent_code IS ?", Integer.class, group, b.parentCode());
        jdbc.update("INSERT INTO common_code(code, group_code, parent_code, sort_no) VALUES (?,?,?,?)", code, group, b.parentCode(), sort);
        putLabel("code." + code, "ko", Valid.text(b.ko(), 50, "name"));
        String en = Valid.optText(b.en(), 50, "name");
        if (en != null) putLabel("code." + code, "en", en);
        codes.invalidate();
        return ResponseEntity.noContent().build();
    }

    /** 순서·사용여부·이름 중 온 것만. 지우지 않는다(가격·제보가 참조) */
    @Transactional
    @PutMapping("/admin/api/codes/{code}")
    public ResponseEntity<Void> updateCode(@PathVariable String code, @RequestBody CodeReq b) {
        if (codes.find(code) == null) throw new NotFound();
        if (b.sort() != null) jdbc.update("UPDATE common_code SET sort_no = ? WHERE code = ?", b.sort(), code);
        if (b.useYn() != null) jdbc.update("UPDATE common_code SET use_yn = ? WHERE code = ?", Valid.oneOf(b.useYn(), YN, "useYn"), code);
        if (b.ko() != null) putLabel("code." + code, "ko", Valid.text(b.ko(), 50, "name"));
        if (b.en() != null) putLabel("code." + code, "en", Valid.text(b.en(), 50, "name"));
        codes.invalidate();
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/api/labels")
    public ResponseEntity<Void> label(@RequestBody LabelReq b) {
        if (b.key() == null || jdbc.queryForObject("SELECT COUNT(*) FROM label WHERE key = ?", Integer.class, b.key()) == 0) throw new NotFound();
        putLabel(b.key(), Valid.oneOf(b.lang(), Valid.LANGS, "lang"), Valid.text(b.text(), 500, "text"));
        return ResponseEntity.noContent().build();
    }

    private void putLabel(String key, String lang, String text) {
        jdbc.update("INSERT INTO label(key, lang, text) VALUES (?,?,?) ON CONFLICT(key, lang) DO UPDATE SET text = excluded.text", key, lang, text);
        labels.invalidate();
    }

    private void vendorExists(long id) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM vendor WHERE id = ?", Integer.class, id) == 0) throw new NotFound();
    }

    private static Object[] vendorArgs(VendorReq b, String createdAt) {
        Object[] a = {Valid.text(b.name(), 50, "name"), Valid.optText(b.nameEn(), 50, "name"), Valid.optText(b.phone(), 20, "phone"),
                Valid.text(b.address(), 200, "address"), Valid.optText(b.intro(), 1000, "intro"),
                Valid.oneOf(b.sampleYn(), YN, "sampleYn"), Valid.oneOf(b.useYn(), YN, "useYn")};
        if (createdAt == null) return a;
        Object[] c = Arrays.copyOf(a, a.length + 1);
        c[a.length] = createdAt;
        return c;
    }
}
