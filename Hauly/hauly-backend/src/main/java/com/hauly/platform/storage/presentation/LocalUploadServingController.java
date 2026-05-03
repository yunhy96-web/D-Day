package com.hauly.platform.storage.presentation;

import com.hauly.platform.storage.infrastructure.LocalFileBlobStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMapping.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Serves files from {@link LocalFileBlobStorage} for the local/dev profile.
 * Active only when a LocalFileBlobStorage bean exists (i.e. {@code hauly.storage.type=local}).
 *
 * The S3 profile uses presigned URLs that bypass this controller entirely.
 */
@RestController
@RequestMapping("/api/intake/uploads/serve")
@ConditionalOnBean(LocalFileBlobStorage.class)
public class LocalUploadServingController {

    private final LocalFileBlobStorage storage;

    public LocalUploadServingController(LocalFileBlobStorage storage) {
        this.storage = storage;
    }

    @GetMapping("/**")
    public ResponseEntity<Resource> serve(HttpServletRequest request) throws Exception {
        // Strip the controller's prefix to get the storage key.
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (fullPath == null) fullPath = request.getRequestURI();
        String prefix = "/api/intake/uploads/serve/";
        int idx = fullPath.indexOf(prefix);
        String key = (idx >= 0) ? fullPath.substring(idx + prefix.length()) : fullPath;
        // Decode each segment
        key = java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);

        Path file = storage.rootDir().resolve(key).normalize();
        if (!file.startsWith(storage.rootDir()) || !Files.exists(file) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaTypeFactory.getMediaType(file.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(Files.size(file))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(new FileSystemResource(file));
    }
}
