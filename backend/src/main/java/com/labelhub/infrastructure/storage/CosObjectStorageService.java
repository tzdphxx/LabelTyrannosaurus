package com.labelhub.infrastructure.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.ResponseHeaderOverrides;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    public InputStream openReadStream(String bucket, String objectKey) {
        // COSObjectInputStream 绑定底层 HTTP 连接，调用方必须在解析完成后关闭。
        return cosClient.getObject(bucket, objectKey).getObjectContent();
    }

    @Override
    public URL generatePresignedDownloadUrl(String bucket, String objectKey, String originalFilename, Instant expiresAt) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey, HttpMethodName.GET);
        request.setExpiration(Date.from(expiresAt));
        request.setResponseHeaders(new ResponseHeaderOverrides()
                .withContentDisposition(buildContentDisposition(originalFilename)));
        return cosClient.generatePresignedUrl(request);
    }

    private String buildContentDisposition(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "attachment";
        }
        String headerFilename = originalFilename.replaceAll("[\\r\\n]", "_");
        String quotedFallback = headerFilename
                .replace("\\", "_")
                .replace("\"", "_")
                .replaceAll("[^\\x20-\\x7E]", "_");
        String encodedFilename = URLEncoder.encode(headerFilename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"%s\"; filename*=UTF-8''%s".formatted(quotedFallback, encodedFilename);
    }
}
