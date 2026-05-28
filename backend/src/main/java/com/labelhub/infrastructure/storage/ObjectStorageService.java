package com.labelhub.infrastructure.storage;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;

public interface ObjectStorageService {

    void upload(String bucket, String objectKey, String contentType, InputStream content, long contentLength);

    URL generatePresignedDownloadUrl(String bucket, String objectKey, String originalFilename, Instant expiresAt);
}
