package com.labelhub.modules.storage.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "labelhub.storage")
public record FileStorageProperties(String bucket, long maxFileSizeBytes, Duration signedUrlTtl) {

    public FileStorageProperties {
        if (bucket == null || bucket.isBlank()) {
            bucket = "labelhub-0000000000";
        }
        if (maxFileSizeBytes <= 0) {
            maxFileSizeBytes = 200L * 1024L * 1024L;
        }
        if (signedUrlTtl == null || signedUrlTtl.isZero() || signedUrlTtl.isNegative()) {
            signedUrlTtl = Duration.ofMinutes(10);
        }
    }
}
