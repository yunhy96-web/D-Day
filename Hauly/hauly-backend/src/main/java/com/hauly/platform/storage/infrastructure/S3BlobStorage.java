package com.hauly.platform.storage.infrastructure;

import com.hauly.platform.storage.domain.BlobStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

/**
 * S3-backed blob storage. Active under the prod profile.
 * Authentication uses the AWS default credentials provider chain — when running on EC2 with
 * an attached IAM Role, no static credentials are needed.
 */
public class S3BlobStorage implements BlobStorage {

    private static final Logger log = LoggerFactory.getLogger(S3BlobStorage.class);

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3BlobStorage(S3Client client, S3Presigner presigner, String bucket) {
        this.client = client;
        this.presigner = presigner;
        this.bucket = bucket;
        log.info("S3BlobStorage bucket={}", bucket);
    }

    @Override
    public void put(String key, InputStream data, long contentLength, String contentType) {
        validate(key);
        client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket).key(key).contentType(contentType)
                        .build(),
                RequestBody.fromInputStream(data, contentLength));
    }

    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        validate(key);
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(b -> b.bucket(bucket).key(key))
                .build()).url().toString();
    }

    @Override
    public void copy(String sourceKey, String destinationKey) {
        validate(sourceKey);
        validate(destinationKey);
        client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucket).sourceKey(sourceKey)
                .destinationBucket(bucket).destinationKey(destinationKey)
                .build());
    }

    @Override
    public void delete(String key) {
        validate(key);
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public boolean exists(String key) {
        validate(key);
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    private static void validate(String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.contains("..")) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
    }
}
