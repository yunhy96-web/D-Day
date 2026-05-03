package com.hauly.platform.storage.domain;

import java.io.InputStream;
import java.time.Duration;

/**
 * Storage abstraction for binary objects (images, documents).
 * Two implementations: local filesystem (default profile) and S3 (prod profile).
 *
 * Keys are forward-slash separated paths, e.g. "orders/123/items/45/abc.jpg".
 * They MUST NOT start with "/" or contain "..".
 */
public interface BlobStorage {

    /** Store an object under the given key. Existing object at that key is overwritten. */
    void put(String key, InputStream data, long contentLength, String contentType);

    /**
     * Returns a URL the frontend can use to GET the object directly.
     * - Local impl: a path on this server, e.g. "/api/intake/uploads/serve/{key}".
     * - S3 impl: an absolute S3 presigned URL valid for {@code ttl}.
     */
    String presignedGetUrl(String key, Duration ttl);

    /** Copy an object from one key to another (idempotent: target overwritten). */
    void copy(String sourceKey, String destinationKey);

    /** Delete an object. No-op if absent. */
    void delete(String key);

    /** Check whether an object exists. */
    boolean exists(String key);
}
