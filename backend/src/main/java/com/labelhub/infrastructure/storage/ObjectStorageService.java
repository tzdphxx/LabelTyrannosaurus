package com.labelhub.infrastructure.storage;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;

public interface ObjectStorageService {

    void upload(String bucket, String objectKey, String contentType, InputStream content, long contentLength);

    InputStream openReadStream(String bucket, String objectKey);

    URL generatePresignedDownloadUrl(String bucket, String objectKey, String originalFilename, Instant expiresAt);
}
