package com.labelhub.infrastructure.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;

@Service
public class CosObjectStorageService implements ObjectStorageService {

    private final COSClient cosClient;

    public CosObjectStorageService(COSClient cosClient) {
        this.cosClient = cosClient;
    }

    @Override
    public void upload(String bucket, String objectKey, String contentType, InputStream content, long contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        metadata.setContentType(contentType);
        cosClient.putObject(bucket, objectKey, content, metadata);
    }

    @Override
    public URL generatePresignedDownloadUrl(String bucket, String objectKey, String originalFilename, Instant expiresAt) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey, HttpMethodName.GET);
        request.setExpiration(Date.from(expiresAt));
        return cosClient.generatePresignedUrl(request);
    }
}
