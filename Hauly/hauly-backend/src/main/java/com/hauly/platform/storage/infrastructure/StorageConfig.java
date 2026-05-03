package com.hauly.platform.storage.infrastructure;

import com.hauly.platform.storage.domain.BlobStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Storage wiring — switches implementation by {@code hauly.storage.type}:
 *  - {@code local} (default): {@link LocalFileBlobStorage} rooted at {@code hauly.storage.local.root}
 *  - {@code s3}: {@link S3BlobStorage} pointing at {@code hauly.storage.s3.bucket}
 */
@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "hauly.storage.type", havingValue = "local", matchIfMissing = true)
    public BlobStorage localBlobStorage(
            @Value("${hauly.storage.local.root:./uploads}") String root) {
        Path rootDir = Paths.get(root);
        return new LocalFileBlobStorage(rootDir);
    }

    @Bean
    @ConditionalOnProperty(name = "hauly.storage.type", havingValue = "s3")
    public BlobStorage s3BlobStorage(
            @Value("${hauly.storage.s3.bucket}") String bucket,
            @Value("${hauly.storage.s3.region:ap-northeast-2}") String region) {
        Region awsRegion = Region.of(region);
        S3Client client = S3Client.builder().region(awsRegion).build();
        S3Presigner presigner = S3Presigner.builder().region(awsRegion).build();
        return new S3BlobStorage(client, presigner, bucket);
    }
}
