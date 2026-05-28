package com.labelhub.infrastructure.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjectStorageServiceTest {

    private final COSClient cosClient = mock(COSClient.class);
    private final ObjectStorageService objectStorageService = new CosObjectStorageService(cosClient);

    @Test
    void uploadPassesMetadataToCos() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("content".getBytes());

        objectStorageService.upload("bucket-1", "uploads/dataset/file.jsonl", "application/x-ndjson", inputStream, 7L);

        ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
        verify(cosClient).putObject(eq("bucket-1"), eq("uploads/dataset/file.jsonl"), eq(inputStream), metadataCaptor.capture());
        ObjectMetadata metadata = metadataCaptor.getValue();
        assertThat(metadata.getContentType()).isEqualTo("application/x-ndjson");
        assertThat(metadata.getContentLength()).isEqualTo(7L);
    }

    @Test
    void generatePresignedDownloadUrlUsesGetMethodAndExpiration() throws Exception {
        when(cosClient.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenReturn(new URL("https://cos.example.com/signed"));
        Instant expiresAt = Instant.parse("2026-05-28T10:00:00Z");

        URL url = objectStorageService.generatePresignedDownloadUrl("bucket-1", "uploads/dataset/file.jsonl",
                "dataset.jsonl", expiresAt);

        ArgumentCaptor<GeneratePresignedUrlRequest> requestCaptor = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
        verify(cosClient).generatePresignedUrl(requestCaptor.capture());
        GeneratePresignedUrlRequest request = requestCaptor.getValue();
        assertThat(url.toString()).isEqualTo("https://cos.example.com/signed");
        assertThat(request.getBucketName()).isEqualTo("bucket-1");
        assertThat(request.getKey()).isEqualTo("uploads/dataset/file.jsonl");
        assertThat(request.getMethod()).isEqualTo(HttpMethodName.GET);
        assertThat(request.getExpiration().toInstant()).isEqualTo(expiresAt);
        assertThat(request.getResponseHeaders().getContentDisposition())
                .contains("attachment")
                .contains("dataset.jsonl");
    }
}
