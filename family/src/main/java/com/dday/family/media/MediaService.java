package com.dday.family.media;

import com.dday.family.auth.Me;
import com.dday.family.common.BadRequest;
import com.dday.family.common.Images;
import com.dday.family.common.ObjectStore;
import com.dday.family.common.Rs;
import com.dday.family.common.Times;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 사진 저장 (설계 §8). 브라우저가 보기용(2560)·썸네일(600)을 만들어 원본과 함께 보낸다.
 * 보기용·썸네일은 다시 인코딩(EXIF·GPS 제거), 원본은 손대지 않고 버킷에 보관.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {
    static final int MAX_VIEW = 8 * 1024 * 1024, MAX_THUMB = 1024 * 1024, MAX_ORIGINAL = 40 * 1024 * 1024;
    private static final Map<String, String> MIME = Map.of("jpg", "image/jpeg", "png", "image/png", "heic", "image/heic");
    private final JdbcTemplate jdbc;
    private final ObjectStore store;

    public record Media(long id, Long postId, long memberId, String thumb, String view, Integer width, Integer height, String takenAt) {}

    public static String viewKey(long fid, String file) { return fid + "/view/" + file + ".jpg"; }
    public static String thumbKey(long fid, String file) { return fid + "/thumb/" + file + ".jpg"; }
    public static String originalKey(long fid, String original) { return fid + "/original/" + original; }

    public long upload(Me me, int viewLen, int thumbLen, String takenAt, byte[] body) throws IOException {
        if (viewLen <= 0 || viewLen > MAX_VIEW || thumbLen <= 0 || thumbLen > MAX_THUMB || body.length <= viewLen + thumbLen) throw new BadRequest("size");
        byte[] o = Arrays.copyOfRange(body, viewLen + thumbLen, body.length);
        BufferedImage vi = Images.decodeJpeg(Arrays.copyOfRange(body, 0, viewLen), 2600);
        BufferedImage ti = Images.decodeJpeg(Arrays.copyOfRange(body, viewLen, viewLen + thumbLen), 700);
        String ext = Images.originalExt(o);
        if (ext == null) throw new BadRequest("original");
        String file = UUID.randomUUID().toString();
        long fid = me.familyId();
        store.put(originalKey(fid, file + "." + ext), o, MIME.get(ext));
        store.put(thumbKey(fid, file), Images.encodeJpeg(ti, 700, 0.85f), "image/jpeg");
        store.put(viewKey(fid, file), Images.encodeJpeg(vi, 2600, 0.9f), "image/jpeg");
        return jdbc.queryForObject("""
                INSERT INTO media(family_id, member_id, kind, file, original, mime, size_bytes, width, height, sha256, taken_at, created_at)
                VALUES (?,?, 'photo', ?,?,?,?,?,?,?,?,?) RETURNING id""", Long.class,
                fid, me.memberId(), file, file + "." + ext, MIME.get(ext), o.length, vi.getWidth(), vi.getHeight(), sha256(o), takenAt(takenAt), Times.now());
    }

    /** 'yyyy-MM-ddTHH:mm:ss' (EXIF, 현지 시각) 또는 ISO 순간. 이상하면 버림 */
    private static String takenAt(String s) {
        if (s == null || s.isBlank() || s.length() > 30) return null;
        try { return Instant.parse(s).toString(); } catch (DateTimeParseException ignored) {}
        try { return java.time.LocalDateTime.parse(s).toString(); } catch (DateTimeParseException ignored) {}
        return null;
    }

    public List<Media> ofPosts(long fid, List<Long> postIds) {
        if (postIds.isEmpty()) return List.of();
        String in = String.join(",", postIds.stream().map(x -> "?").toList());
        Object[] args = new Object[postIds.size() + 1];
        args[0] = fid;
        for (int i = 0; i < postIds.size(); i++) args[i + 1] = postIds.get(i);
        return jdbc.query("SELECT id, post_id, member_id, file, width, height, taken_at FROM media WHERE family_id=? AND post_id IN (" + in + ") ORDER BY post_id, sort, id",
                (rs, i) -> map(fid, rs.getLong(1), Rs.lng(rs, 2), rs.getLong(3), rs.getString(4), Rs.integer(rs, 5), Rs.integer(rs, 6), rs.getString(7)), args);
    }

    public Media map(long fid, long id, Long postId, long memberId, String file, Integer w, Integer h, String takenAt) {
        return new Media(id, postId, memberId, store.url(thumbKey(fid, file)), store.url(viewKey(fid, file)), w, h, takenAt);
    }

    /** 원본 키. 본인 또는 media.original.download 권한. 없거나 못 보면 null */
    public String originalKey(Me me, long id, boolean anyAllowed) {
        List<String[]> r = jdbc.query("SELECT original, member_id FROM media WHERE id=? AND family_id=?",
                (rs, i) -> new String[]{rs.getString(1), rs.getString(2)}, id, me.familyId());
        if (r.isEmpty()) return null;
        if (!anyAllowed && Long.parseLong(r.get(0)[1]) != me.memberId()) return null;
        return originalKey(me.familyId(), r.get(0)[0]);
    }

    /** 글에 안 붙은 채 하루 지난 업로드 정리 (MaintenanceJob) */
    public int cleanupOrphans() {
        List<Object[]> rows = jdbc.query("SELECT id, family_id, file, original FROM media WHERE post_id IS NULL AND created_at < ?",
                (rs, i) -> new Object[]{rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4)}, Times.ago(86_400));
        for (Object[] r : rows) {
            long fid = (long) r[1];
            try {
                store.delete(viewKey(fid, (String) r[2]));
                store.delete(thumbKey(fid, (String) r[2]));
                store.delete(originalKey(fid, (String) r[3]));
                jdbc.update("DELETE FROM media WHERE id=?", r[0]);
            } catch (IOException | RuntimeException e) {
                log.warn("orphan media {} cleanup failed", r[0], e);
            }
        }
        return rows.size();
    }

    static String sha256(byte[] b) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
