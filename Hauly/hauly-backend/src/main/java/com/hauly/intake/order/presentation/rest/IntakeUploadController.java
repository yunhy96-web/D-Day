package com.hauly.intake.order.presentation.rest;

import com.hauly.platform.storage.domain.BlobStorage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Temporary upload endpoint used by the order-create flow.
 *
 * Flow:
 * 1. Client uploads each image as the user picks it → POST /api/intake/uploads/temp
 * 2. Server stores under {@code temp/{userId}/{uuid}.{ext}} and returns {tempKey, url}
 * 3. Client collects the tempKeys per item; on order create they go in the request body
 * 4. {@link com.hauly.intake.order.application.IntakeOrderService#createOrder} copies each
 *    temp key under {@code orders/{orderId}/items/{itemId}/{uuid}.{ext}} and deletes the temp.
 * 5. Orphan temp files are cleaned by a scheduled cron after 24h.
 */
@RestController
@RequestMapping("/api/intake/uploads")
@PreAuthorize("hasRole('ADMIN')")
public class IntakeUploadController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic", "image/heif");
    private static final Duration TEMP_URL_TTL = Duration.ofMinutes(30);

    private final BlobStorage storage;

    public IntakeUploadController(BlobStorage storage) {
        this.storage = storage;
    }

    @PostMapping(value = "/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadTemp(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long userId) throws IOException {

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "unsupported_type",
                    "contentType", contentType == null ? "" : contentType));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "empty_file"));
        }

        String ext = extensionFor(contentType, file.getOriginalFilename());
        String tempKey = "temp/" + userId + "/" + UUID.randomUUID() + "." + ext;

        try (var in = file.getInputStream()) {
            storage.put(tempKey, in, file.getSize(), contentType);
        }

        String url = storage.presignedGetUrl(tempKey, TEMP_URL_TTL);
        return ResponseEntity.ok(Map.of("tempKey", tempKey, "url", url));
    }

    /**
     * Cancel an in-progress upload. Only the uploader (or ADMIN) can delete their own temp.
     * The path-segment scheme is {@code temp/{userId}/...}, so we enforce ownership by prefix.
     */
    @DeleteMapping("/temp/**")
    public ResponseEntity<Void> deleteTemp(@AuthenticationPrincipal Long userId,
                                            jakarta.servlet.http.HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String prefix = "/api/intake/uploads/temp/";
        int idx = fullPath.indexOf(prefix);
        if (idx < 0) return ResponseEntity.notFound().build();
        String key = "temp/" + fullPath.substring(idx + prefix.length());
        // Decode any URL encoding
        key = java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);

        // Ownership check: temp/{userId}/... must match the caller
        String expected = "temp/" + userId + "/";
        if (!key.startsWith(expected)) {
            return ResponseEntity.status(403).build();
        }

        storage.delete(key);
        return ResponseEntity.noContent().build();
    }

    private static String extensionFor(String contentType, String originalName) {
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                String fromName = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (fromName.matches("[a-z0-9]{1,5}")) return fromName;
            }
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/heic" -> "heic";
            case "image/heif" -> "heif";
            default -> "bin";
        };
    }
}
