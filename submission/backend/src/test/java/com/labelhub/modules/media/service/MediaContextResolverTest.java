package com.labelhub.modules.media.service;

import com.labelhub.infrastructure.storage.ObjectStorageService;
import com.labelhub.modules.media.domain.DatasetItemMediaContextEntity;
import com.labelhub.modules.media.mapper.DatasetItemMediaContextMapper;
import com.labelhub.modules.storage.domain.ObjectFileEntity;
import com.labelhub.modules.storage.repository.ObjectFileMapper;
import com.labelhub.modules.storage.service.FileStorageProperties;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediaContextResolverTest {

    private final DatasetItemMediaContextMapper contextMapper = mock(DatasetItemMediaContextMapper.class);
    private final ObjectFileMapper objectFileMapper = mock(ObjectFileMapper.class);
    private final ObjectStorageService objectStorageService = mock(ObjectStorageService.class);
    private final MediaContextResolver resolver = new MediaContextResolver(
            contextMapper,
            objectFileMapper,
            objectStorageService,
            new FileStorageProperties("labelhub-test", 1024, Duration.ofMinutes(10))
    );

    @Test
    void resolvesUploadedImageFileToSignedUrlWithoutLosingContextMetadata() throws Exception {
        DatasetItemMediaContextEntity context = new DatasetItemMediaContextEntity();
        context.setDatasetItemId(100L);
        context.setMediaType("image");
        context.setProcessingStatus("READY");
        context.setContextJson("""
                {"media_type":"image","media_file_id":99,"owner_media_description":"cat sitting"}
                """);
        context.setLimitationsJson("[]");
        when(contextMapper.selectLatestByDatasetItemId(100L)).thenReturn(context);
        when(objectFileMapper.selectById(99L)).thenReturn(objectFile(99L, "uploads/media/cat.png", "cat.png"));
        when(objectStorageService.generatePresignedDownloadUrl(eq("labelhub-test"), eq("uploads/media/cat.png"),
                eq("cat.png"), any())).thenReturn(new URL("https://cos.example.com/signed-cat.png?token=secret"));

        String resolved = resolver.resolveItemJson(100L, "{\"media_type\":\"text\",\"text\":\"fallback\"}");

        assertThat(resolved).contains("\"media_url\":\"https://cos.example.com/signed-cat.png?token=secret\"");
        assertThat(resolved).contains("\"media_processing_status\":\"READY\"");
        assertThat(resolved).contains("\"owner_media_description\":\"cat sitting\"");
    }

    @Test
    void fallsBackToOriginalItemJsonWhenNoMediaContextExists() {
        when(contextMapper.selectLatestByDatasetItemId(100L)).thenReturn(null);

        String resolved = resolver.resolveItemJson(100L, "{\"media_type\":\"image\",\"media_url\":\"https://e.com/a.png\"}");

        assertThat(resolved).isEqualTo("{\"media_type\":\"image\",\"media_url\":\"https://e.com/a.png\"}");
    }

    private static ObjectFileEntity objectFile(Long id, String objectKey, String filename) {
        ObjectFileEntity entity = new ObjectFileEntity();
        entity.setId(id);
        entity.setBucketName("labelhub-test");
        entity.setObjectKey(objectKey);
        entity.setOriginalFilename(filename);
        entity.setContentType("image/png");
        entity.setFileSize(10L);
        return entity;
    }
}
