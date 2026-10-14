package com.dday.family.job;

import com.dday.family.common.AppProps;
import com.dday.family.common.ObjectStore;
import com.dday.family.common.Times;
import com.dday.family.media.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;

/** 매일 04:00(서울): DB 스냅샷 → 버킷 backup/, 30일 전 것 삭제, 만료 세션·글 없는 업로드 정리 (설계 §12). */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceJob {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final JdbcTemplate jdbc;
    private final ObjectStore store;
    private final MediaService media;
    private final AppProps props;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void daily() {
        backup();
        int s = jdbc.update("DELETE FROM session WHERE expires_at < ?", Times.now());
        int m = media.cleanupOrphans();
        log.info("maintenance: sessions -{}, orphan media -{}", s, m);
    }

    public void backup() {
        LocalDate today = LocalDate.now(SEOUL);
        Path tmp = Path.of(props.getDataDir(), "backup-tmp.db");
        try {
            Files.deleteIfExists(tmp);
            jdbc.execute("VACUUM INTO '" + tmp.toAbsolutePath().toString().replace("'", "''") + "'");
            store.put("backup/family-" + today + ".db", tmp, "application/octet-stream");
            store.delete("backup/family-" + today.minusDays(30) + ".db");
            log.info("backup ok: {} bytes", Files.size(tmp));
        } catch (Exception e) {
            log.error("backup failed", e);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }
}
