package com.hauly.platform.storage.infrastructure;

import com.hauly.platform.storage.domain.BlobStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Filesystem-backed blob storage for local/dev profiles.
 * Files live under {@code rootDir} and are served back through
 * {@code /api/intake/uploads/serve/...} (see {@link com.hauly.platform.storage.presentation.LocalUploadServingController}).
 *
 * The {@code ttl} on {@link #presignedGetUrl} is ignored — the URL is just a path on this server.
 */
public class LocalFileBlobStorage implements BlobStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileBlobStorage.class);

    private final Path rootDir;

    public LocalFileBlobStorage(Path rootDir) {
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize local upload directory: " + rootDir, e);
        }
        this.rootDir = rootDir.toAbsolutePath().normalize();
        log.info("LocalFileBlobStorage rooted at {}", this.rootDir);
    }

    @Override
    public void put(String key, InputStream data, long contentLength, String contentType) {
        Path target = resolveSafe(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + key, e);
        }
    }

    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        // For local dev we serve through Spring; ignore ttl.
        // URL-encode each path segment so spaces / unicode survive.
        String[] parts = key.split("/");
        StringBuilder sb = new StringBuilder("/api/intake/uploads/serve");
        for (String p : parts) {
            sb.append('/').append(URLEncoder.encode(p, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return sb.toString();
    }

    @Override
    public void copy(String sourceKey, String destinationKey) {
        Path src = resolveSafe(sourceKey);
        Path dst = resolveSafe(destinationKey);
        try {
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy " + sourceKey + " -> " + destinationKey, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolveSafe(key));
        } catch (IOException e) {
            log.warn("Delete failed for {}: {}", key, e.toString());
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolveSafe(key));
    }

    /** Resolve and reject anything that escapes the root directory (path traversal). */
    private Path resolveSafe(String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.contains("..")) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        Path resolved = rootDir.resolve(key).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new IllegalArgumentException("Key escapes storage root: " + key);
        }
        return resolved;
    }

    /** Test/admin helper — exposes the root for the serving controller. */
    public Path rootDir() {
        return rootDir;
    }
}
