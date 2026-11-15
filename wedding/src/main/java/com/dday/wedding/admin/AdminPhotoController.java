package com.dday.wedding.admin;

import com.dday.wedding.common.BadRequest;
import com.dday.wedding.common.Images;
import com.dday.wedding.common.ObjectStore;
import com.dday.wedding.guestbook.GuestbookController;
import com.dday.wedding.photo.PhotoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** 설계 §15-6·§15-7. 갤러리·월드컵 사진 + 방명록 관리. 인증은 AdminInterceptor. */
@RestController
public class AdminPhotoController {
    private static final int MAX_VIEW = 8 * 1024 * 1024, MAX_THUMB = 1024 * 1024, MAX_ORIGINAL = 40 * 1024 * 1024;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final PhotoService photos;
    private final ObjectStore store;
    private final Path originalDir;   // 원본은 Pi 로컬에만(비공개)

    public AdminPhotoController(JdbcTemplate jdbc, TransactionTemplate tx, PhotoService photos, ObjectStore store, @Value("${app.data-dir}") String dataDir) {
        this.jdbc = jdbc; this.tx = tx; this.photos = photos; this.store = store;
        this.originalDir = Path.of(dataDir, "photos", "original");
    }

    public record Order(List<Long> ids) {}
    /** 여러 장 한 번에. null 인 항목은 안 바꾼다 */
    public record Bulk(List<Long> ids, Boolean visible, Boolean worldcup) {}
    public record Ids(List<Long> ids) {}
    public record Hide(Boolean hidden) {}

    // ---- 갤러리·월드컵 사진

    @GetMapping("/admin/api/photos")
    public List<PhotoService.Photo> list() { return photos.adminAll(); }

    /** body = 보기용 | 썸네일 | 원본. 앞 두 개 길이는 쿼리로. 셋 다 브라우저가 만든다(EXIF 회전 반영, Pi 메모리 보호) */
    @PostMapping(value = "/admin/api/photos", consumes = "application/octet-stream")
    public Map<String, Object> upload(@RequestParam int view, @RequestParam int thumb, @RequestParam(defaultValue = "Y") String worldcup,
                                      @RequestParam(defaultValue = "Y") String visible, HttpServletRequest req) throws IOException {
        if (view <= 0 || view > MAX_VIEW || thumb <= 0 || thumb > MAX_THUMB) throw new BadRequest("size");
        byte[] body = Images.read(req.getInputStream(), view + thumb + MAX_ORIGINAL);
        if (body.length <= view + thumb) throw new BadRequest("size");
        byte[] v = Arrays.copyOfRange(body, 0, view), t = Arrays.copyOfRange(body, view, view + thumb), o = Arrays.copyOfRange(body, view + thumb, body.length);
        Images.decodeJpeg(v, 2600); Images.decodeJpeg(t, 700);
        String ext = Images.originalExt(o);
        if (ext == null) throw new BadRequest("original");
        String file = UUID.randomUUID().toString();
        Images.save(originalDir, file + "." + ext, o);
        store.put(PhotoService.thumbKey(file), t);
        store.put(PhotoService.viewKey(file), v);
        Long id = jdbc.queryForObject("INSERT INTO photo(file, original, sort, worldcup_yn) VALUES (?, ?, (SELECT COALESCE(MAX(sort), 0) + 1 FROM photo), ?) RETURNING id",
                Long.class, file, file + "." + ext, "N".equals(worldcup) ? "N" : "Y");
        if ("N".equals(visible)) jdbc.update("INSERT OR IGNORE INTO photo_hidden(photo_id) VALUES (?)", id);
        return Map.of("id", id);
    }

    /** 드래그 결과 전체 순서. 첫 번째가 메인 */
    @PutMapping("/admin/api/photos/order")
    public ResponseEntity<Void> order(@RequestBody Order o) {
        if (o.ids() == null || o.ids().isEmpty() || o.ids().size() > 1000 || new HashSet<>(o.ids()).size() != o.ids().size()) throw new BadRequest("ids");
        tx.executeWithoutResult(s -> {
            for (int i = 0; i < o.ids().size(); i++) jdbc.update("UPDATE photo SET sort=? WHERE id=?", i + 1, o.ids().get(i));
        });
        return ResponseEntity.noContent().build();
    }

    /** 노출·월드컵 일괄 변경 */
    @PutMapping("/admin/api/photos/bulk")
    public ResponseEntity<Void> bulk(@RequestBody Bulk b) {
        List<Long> ids = ids(b.ids());
        if (b.visible() == null && b.worldcup() == null) throw new BadRequest("nothing");
        tx.executeWithoutResult(s -> {
            for (Long id : ids) {
                if (b.worldcup() != null) jdbc.update("UPDATE photo SET worldcup_yn=? WHERE id=?", b.worldcup() ? "Y" : "N", id);
                if (Boolean.TRUE.equals(b.visible())) jdbc.update("DELETE FROM photo_hidden WHERE photo_id=?", id);
                if (Boolean.FALSE.equals(b.visible())) jdbc.update("INSERT OR IGNORE INTO photo_hidden(photo_id) SELECT id FROM photo WHERE id=?", id);
            }
        });
        return ResponseEntity.noContent().build();
    }

    /** 일괄 삭제(원본·보기용·썸네일 파일까지). DB 를 먼저 지우고 파일은 그다음 — 파일 삭제가 실패해도 화면엔 안 남는다 */
    @PostMapping("/admin/api/photos/delete")
    public Map<String, Object> delete(@RequestBody Ids body) throws IOException {
        List<Long> ids = ids(body.ids());
        List<String[]> rows = new ArrayList<>();
        tx.executeWithoutResult(s -> {
            for (Long id : ids) {
                rows.addAll(jdbc.query("SELECT file, original FROM photo WHERE id=?", (rs, i) -> new String[]{rs.getString(1), rs.getString(2)}, id));
                jdbc.update("DELETE FROM photo WHERE id=?", id);
                jdbc.update("DELETE FROM photo_hidden WHERE photo_id=?", id);
            }
        });
        for (String[] r : rows) {
            store.delete(PhotoService.viewKey(r[0]));
            store.delete(PhotoService.thumbKey(r[0]));
            Files.deleteIfExists(originalDir.resolve(r[1]));
        }
        return Map.of("deleted", rows.size());
    }

    private static List<Long> ids(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 1000 || ids.contains(null)) throw new BadRequest("ids");
        return ids.stream().distinct().toList();
    }

    // ---- 방명록 관리 (숨김 포함 전체)

    @GetMapping("/admin/api/guestbook")
    public Map<String, Object> guestbook(@RequestParam(defaultValue = "0") int page) {
        int size = 30;
        List<Map<String, Object>> rows = jdbc.query("SELECT id, name, message, deleted_yn, created_at FROM guestbook ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong("id")); m.put("name", rs.getString("name")); m.put("message", rs.getString("message"));
                    m.put("hidden", "Y".equals(rs.getString("deleted_yn"))); m.put("createdAt", rs.getString("created_at"));
                    return m;
                }, size + 1, Math.max(0, page) * size);
        boolean hasNext = rows.size() > size;
        if (hasNext) rows = rows.subList(0, size);
        for (Map<String, Object> r : rows)
            r.put("photos", jdbc.query("SELECT id, file FROM guestbook_photo WHERE guestbook_id=? ORDER BY id",
                    (rs, i) -> Map.of("id", rs.getLong("id"), "thumb", store.url(GuestbookController.thumbKey(rs.getString("file")))), r.get("id")));
        return Map.of("items", rows, "hasNext", hasNext);
    }

    @PutMapping("/admin/api/guestbook/{id}")
    public ResponseEntity<Void> hide(@PathVariable long id, @RequestBody Hide h) {
        if (h.hidden() == null) throw new BadRequest("hidden");
        int n = jdbc.update("UPDATE guestbook SET deleted_yn=? WHERE id=?", h.hidden() ? "Y" : "N", id);
        return n == 0 ? ResponseEntity.notFound().build() : ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/api/guestbook/photo/{photoId}")
    public ResponseEntity<Void> deleteGuestPhoto(@PathVariable long photoId) throws IOException {
        List<String> f = jdbc.query("SELECT file FROM guestbook_photo WHERE id=?", (rs, i) -> rs.getString(1), photoId);
        if (f.isEmpty()) return ResponseEntity.notFound().build();
        jdbc.update("DELETE FROM guestbook_photo WHERE id=?", photoId);
        store.delete(GuestbookController.viewKey(f.get(0)));
        store.delete(GuestbookController.thumbKey(f.get(0)));
        return ResponseEntity.noContent().build();
    }
}
