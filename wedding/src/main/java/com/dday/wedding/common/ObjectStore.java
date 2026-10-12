package com.dday.wedding.common;

import com.dday.wedding.config.WeddingProps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 하객에게 나가는 사진 파일 (설계 §15-10). 키 = data 디렉토리 기준 상대경로.
 * Ncloud Storage(비공개 버킷) + 서명 URL. 접근키가 없으면 로컬 디스크 + /files/** (개발용). 원본은 여기로 오지 않는다.
 */
@Slf4j
@Component
public class ObjectStore {
    /** 하객 사진 키가 생기는 디렉토리. 기동 시 로컬 → 버킷 1회 이전 대상 */
    static final List<String> PUBLIC_DIRS = List.of("photos/view", "photos/thumb", "timeline", "guestbook");
    private record Signed(String url, long day) {}

    private final Path dataDir;
    private final String bucket;
    private final S3Client s3;
    private final S3Presigner presigner;
    private final Map<String, Signed> signed = new ConcurrentHashMap<>();

    public ObjectStore(@Value("${app.data-dir}") String dataDir, WeddingProps props) {
        this.dataDir = Path.of(dataDir);
        WeddingProps.Storage st = props.getStorage();
        this.bucket = st.getBucket();
        if (st.getAccessKey().isBlank()) { s3 = null; presigner = null; return; }
        var creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(st.getAccessKey(), st.getSecretKey()));
        URI endpoint = URI.create(st.getEndpoint());
        Region region = Region.of(st.getRegion());
        s3 = S3Client.builder().httpClient(UrlConnectionHttpClient.create()).endpointOverride(endpoint).region(region).credentialsProvider(creds).build();
        presigner = S3Presigner.builder().endpointOverride(endpoint).region(region).credentialsProvider(creds).build();
    }

    public boolean remote() { return s3 != null; }

    public void put(String key, byte[] jpeg) throws IOException {
        if (s3 == null) { Path p = dataDir.resolve(key); Images.save(p.getParent(), p.getFileName().toString(), jpeg); return; }
        s3.putObject(b -> b.bucket(bucket).key(key).contentType("image/jpeg").cacheControl("max-age=604800, immutable"), RequestBody.fromBytes(jpeg));
    }

    public void delete(String key) throws IOException {
        if (s3 == null) { Files.deleteIfExists(dataDir.resolve(key)); return; }
        s3.deleteObject(b -> b.bucket(bucket).key(key));
    }

    /**
     * 7일짜리 서명 URL. 같은 키는 하루 동안 같은 URL 을 돌려줘 브라우저 캐시가 먹게 한다.
     * ponytail: 키 2만 개 넘으면 캐시 통째로 비움. 이 규모(사진 수백 장)에선 안 닿는다.
     */
    public String url(String key) {
        if (s3 == null) return "/files/" + key;
        long day = System.currentTimeMillis() / 86_400_000L;
        Signed s = signed.get(key);
        if (s != null && s.day() == day) return s.url();
        if (signed.size() > 20_000) signed.clear();
        String u = presigner.presignGetObject(r -> r.signatureDuration(Duration.ofDays(7))
                .getObjectRequest(g -> g.bucket(bucket).key(key))).url().toString();
        signed.put(key, new Signed(u, day));
        return u;
    }

    /** 버킷 연결 전에 로컬에 쌓인 사진을 1회 올린다. 성공하면 data/.migrated-ncloud 표식. 로컬 파일은 백업으로 남김 */
    @EventListener(ApplicationReadyEvent.class)
    public void migrateLocal() {
        Path marker = dataDir.resolve(".migrated-ncloud");
        if (s3 == null || Files.exists(marker)) return;
        int n = 0;
        try {
            for (String dir : PUBLIC_DIRS) {
                Path d = dataDir.resolve(dir);
                if (!Files.isDirectory(d)) continue;
                try (Stream<Path> files = Files.list(d)) {
                    for (Path f : files.filter(p -> p.toString().endsWith(".jpg")).toList()) {
                        put(dir + "/" + f.getFileName(), Files.readAllBytes(f)); n++;
                    }
                }
            }
            Files.writeString(marker, "uploaded " + n + " files\n");
            log.info("local photos → Ncloud Storage: {} files", n);
        } catch (Exception e) {
            log.error("local → Ncloud migration failed after {} files (다음 기동 때 다시 시도)", n, e);
        }
    }
}
