package com.dday.family.media;

import com.dday.family.auth.Audit;
import com.dday.family.auth.Me;
import com.dday.family.auth.Perm;
import com.dday.family.common.Images;
import com.dday.family.common.NotFound;
import com.dday.family.common.ObjectStore;
import com.dday.family.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class MediaController {
    private final MediaService media;
    private final ObjectStore store;
    private final RateLimiter limiter;
    private final Audit audit;

    /** body = 보기용 | 썸네일 | 원본 바이트. 앞 두 개 길이는 쿼리로 */
    @PostMapping(value = "/api/media", consumes = "application/octet-stream")
    public Map<String, Object> upload(@RequestParam int view, @RequestParam int thumb, @RequestParam(required = false) String takenAt,
                                      Me me, HttpServletRequest req) throws IOException {
        me.require(Perm.MEDIA_UPLOAD);
        if (!limiter.allow("media:" + me.memberId(), 300, 3600)) throw new RateLimiter.TooManyRequests();
        byte[] body = Images.read(req.getInputStream(), MediaService.MAX_VIEW + MediaService.MAX_THUMB + MediaService.MAX_ORIGINAL);
        return Map.of("id", media.upload(me, view, thumb, takenAt, body));
    }

    /** 원본 받기: 올린 사람 또는 권한자. 기록 남김 (설계 §15-3) */
    @GetMapping("/api/media/{id}/original")
    public ResponseEntity<Resource> original(@PathVariable long id, Me me, HttpServletRequest req) {
        String key = media.originalKey(me, id, me.has(Perm.MEDIA_ORIGINAL));
        if (key == null) throw new NotFound();
        audit.log(me, "media.original.download", "media:" + id, req);
        String name = key.substring(key.lastIndexOf('/') + 1);
        if (store.remote()) return ResponseEntity.status(302).location(URI.create(store.downloadUrl(key, name))).build();
        Path p = store.local(key);
        if (!Files.isRegularFile(p)) throw new NotFound();
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(new FileSystemResource(p));
    }

    /** 로컬 모드(Ncloud 키 없음) 전용 보기용·썸네일 서빙. 내 가족 것만. 원본은 위 API 로만 */
    @GetMapping("/files/{fid}/{kind}/{name:.+}")
    public ResponseEntity<Resource> file(@PathVariable long fid, @PathVariable String kind, @PathVariable String name, Me me) throws IOException {
        if (store.remote() || fid != me.familyId() || !(kind.equals("view") || kind.equals("thumb"))) throw new NotFound();
        Path p = store.local(fid + "/" + kind + "/" + name);
        if (!Files.isRegularFile(p)) throw new NotFound();
        String ct = Files.probeContentType(p);
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .contentType(ct == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(ct)).body(new FileSystemResource(p));
    }
}
