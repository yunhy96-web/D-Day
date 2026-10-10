package com.dday.wedding.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** data/photos 디렉토리 = 사진 목록. 파일명 stem 이 사진 ID. 업로드 API 없음(설계 §7). */
@Service
public class PhotoService {
    private final Path dir;
    private volatile List<String> ids = List.of();   // 정렬된 stem
    private volatile Map<String, String> urlById = Map.of();
    private volatile long scannedAt;

    public PhotoService(@Value("${app.data-dir}") String dataDir) { this.dir = Path.of(dataDir, "photos"); }

    /** 60초 캐시. scp 로 사진 바꿔도 재시작 없이 반영. */
    private void scan() {
        if (System.currentTimeMillis() - scannedAt < 60_000) return;
        try (Stream<Path> s = Files.list(dir)) {
            List<String> files = s.map(p -> p.getFileName().toString())
                    .filter(n -> n.matches("(?i)[\\w-]+\\.(webp|jpe?g|png|svg)")).sorted().toList();
            urlById = files.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    n -> n.substring(0, n.lastIndexOf('.')), n -> "/photos/" + n, (a, b) -> a));
            ids = files.stream().map(n -> n.substring(0, n.lastIndexOf('.'))).distinct().toList();
        } catch (IOException e) {
            ids = List.of(); urlById = Map.of();
        }
        scannedAt = System.currentTimeMillis();
    }

    public List<String> ids() { scan(); return ids; }
    public Map<String, String> urls() { scan(); return urlById; }
    public boolean exists(String id) { scan(); return urlById.containsKey(id); }
    public String url(String id) { scan(); return urlById.get(id); }
}
