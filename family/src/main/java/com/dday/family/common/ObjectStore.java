package com.dday.family.common;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 파일 저장소 (설계 §8). Ncloud Storage 비공개 버킷 + 서명 URL.
 * 접근키가 없으면 로컬 디스크 data/store/{key} (개발용, MediaController 가 로그인 확인 후 서빙).
 */
@Component
public class ObjectStore {
    private record Signed(String url, long day) {}

    private final Path localDir;
    private final String bucket;
    private final S3Client s3;
    private final S3Presigner presigner;
    private final Map<String, Signed> signed = new ConcurrentHashMap<>();

    public ObjectStore(AppProps props) {
        this.localDir = Path.of(props.getDataDir(), "store").toAbsolutePath().normalize();
        AppProps.Storage st = props.getStorage();
        this.bucket = st.getBucket();
        if (st.getAccessKey().isBlank()) { s3 = null; presigner = null; return; }
        var creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(st.getAccessKey(), st.getSecretKey()));
        URI endpoint = URI.create(st.getEndpoint());
        Region region = Region.of(st.getRegion());
        s3 = S3Client.builder().httpClient(UrlConnectionHttpClient.create()).endpointOverride(endpoint).region(region).credentialsProvider(creds).build();
        presigner = S3Presigner.builder().endpointOverride(endpoint).region(region).credentialsProvider(creds).build();
    }

    public boolean remote() { return s3 != null; }

    public void put(String key, byte[] data, String contentType) throws IOException {
        if (s3 == null) { Path p = local(key); Images.save(p.getParent(), p.getFileName().toString(), data); return; }
        s3.putObject(b -> b.bucket(bucket).key(key).contentType(contentType).cacheControl("private, max-age=604800, immutable"), RequestBody.fromBytes(data));
    }

    public void put(String key, Path file, String contentType) throws IOException {
        if (s3 == null) { Path p = local(key); Files.createDirectories(p.getParent()); Files.copy(file, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING); return; }
        s3.putObject(b -> b.bucket(bucket).key(key).contentType(contentType), RequestBody.fromFile(file));
    }

    public void delete(String key) throws IOException {
        if (s3 == null) { Files.deleteIfExists(local(key)); return; }
        s3.deleteObject(b -> b.bucket(bucket).key(key));
    }

    /** 로컬 모드 전용. key 가 store 밖으로 못 나가게 막는다 */
    public Path local(String key) {
        Path p = localDir.resolve(key).normalize();
        if (!p.startsWith(localDir)) throw new BadRequest("key");
        return p;
    }

    /**
     * 1일짜리 서명 URL. 같은 키는 같은 날 같은 URL 을 돌려줘 브라우저 캐시가 먹게 한다.
     * ponytail: 키 2만 개 넘으면 캐시 통째로 비움. 가족 규모에선 안 닿는다.
     */
    public String url(String key) {
        if (s3 == null) return "/files/" + key;
        long day = System.currentTimeMillis() / 86_400_000L;
        Signed s = signed.get(key);
        if (s != null && s.day() == day) return s.url();
        if (signed.size() > 20_000) signed.clear();
        String u = presigner.presignGetObject(r -> r.signatureDuration(Duration.ofHours(36))
                .getObjectRequest(g -> g.bucket(bucket).key(key))).url().toString();
        signed.put(key, new Signed(u, day));
        return u;
    }

    /** 원본 받기: 5분짜리, 파일로 저장되게. 원격 전용(로컬은 MediaController 가 직접 전송) */
    public String downloadUrl(String key, String filename) {
        return presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(g -> g.bucket(bucket).key(key).responseContentDisposition("attachment; filename=\"" + filename + "\""))).url().toString();
    }
}
