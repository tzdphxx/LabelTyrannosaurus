package com.labelhub.modules.storage;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.storage.ObjectStorageService;
import com.labelhub.modules.storage.domain.ObjectFileEntity;
import com.labelhub.modules.storage.dto.FileUploadResponse;
import com.labelhub.modules.storage.dto.SignedUrlResponse;
import com.labelhub.modules.storage.repository.ObjectFileMapper;
import com.labelhub.modules.storage.service.FileService;
import com.labelhub.modules.storage.service.FileStorageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileServiceTest {

    private final ObjectFileMapper objectFileMapper = mock(ObjectFileMapper.class);
    private final ObjectStorageService objectStorageService = mock(ObjectStorageService.class);
    private final FileStorageProperties properties = new FileStorageProperties(
            "labelhub-test",
            200L * 1024L * 1024L,
            Duration.ofMinutes(10)
    );
    private final FileService fileService = new FileService(objectFileMapper, objectStorageService, properties);

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void uploadStoresObjectAndPersistsMetadata() throws Exception {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        when(objectStorageService.generatePresignedDownloadUrl(eq("labelhub-test"), any(), eq("dataset.jsonl"), any()))
                .thenReturn(new URL("https://cos.example.com/download"));
        when(objectFileMapper.insert(any(ObjectFileEntity.class))).thenAnswer(invocation -> {
            ObjectFileEntity entity = invocation.getArgument(0);
            entity.setId(99L);
            return 1;
        });
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dataset.jsonl",
                "application/x-ndjson",
                "{\"id\":1}\n".getBytes()
        );

        FileUploadResponse response = fileService.upload(file, "dataset");

        ArgumentCaptor<ObjectFileEntity> entityCaptor = ArgumentCaptor.forClass(ObjectFileEntity.class);
        verify(objectStorageService).upload(eq("labelhub-test"), any(), eq("application/x-ndjson"), any(), eq(file.getSize()));
        verify(objectFileMapper).insert(entityCaptor.capture());
        ObjectFileEntity saved = entityCaptor.getValue();
        assertThat(saved.getOwnerId()).isEqualTo(10L);
        assertThat(saved.getBucketName()).isEqualTo("labelhub-test");
        assertThat(saved.getOriginalFilename()).isEqualTo("dataset.jsonl");
        assertThat(saved.getFileSize()).isEqualTo(file.getSize());
        assertThat(saved.getStorageProvider()).isEqualTo("COS");
        assertThat(saved.getObjectKey()).startsWith("uploads/dataset/");
        assertThat(saved.getObjectKey()).endsWith("-dataset.jsonl");
        assertThat(response.fileId()).isEqualTo(99L);
        assertThat(response.downloadUrl()).isEqualTo("https://cos.example.com/download");
    }

    @Test
    void uploadClosesInputStreamWhenObjectStorageFails() throws Exception {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        AtomicBoolean closed = new AtomicBoolean(false);
        byte[] content = "{\"id\":1}\n".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dataset.jsonl",
                "application/x-ndjson",
                content
        ) {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(content) {
                    @Override
                    public void close() throws java.io.IOException {
                        closed.set(true);
                        super.close();
                    }
                };
            }
        };
        doThrow(new IllegalStateException("cos unavailable"))
                .when(objectStorageService)
                .upload(eq("labelhub-test"), any(), eq("application/x-ndjson"), any(), eq(file.getSize()));

        assertThatThrownBy(() -> fileService.upload(file, "dataset"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(closed).isTrue();
    }

    @Test
    void uploadRejectsEmptyFile() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        MockMultipartFile file = new MockMultipartFile("file", "empty.json", "application/json", new byte[0]);

        assertThatThrownBy(() -> fileService.upload(file, "dataset"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
        verify(objectStorageService, never()).upload(any(), any(), any(), any(), eq(0L));
    }

    @Test
    void uploadRejectsUnsupportedExtension() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> fileService.upload(file, "dataset"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
        verify(objectStorageService, never()).upload(any(), any(), any(), any(), eq(1L));
    }

    @Test
    void ownerCanGenerateSignedUrl() throws Exception {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        when(objectFileMapper.selectById(99L)).thenReturn(objectFile(99L, 10L));
        when(objectStorageService.generatePresignedDownloadUrl(eq("labelhub-test"), eq("uploads/dataset/file.jsonl"),
                eq("dataset.jsonl"), any())).thenReturn(new URL("https://cos.example.com/signed"));

        SignedUrlResponse response = fileService.generateSignedUrl(99L);

        assertThat(response.fileId()).isEqualTo(99L);
        assertThat(response.downloadUrl()).isEqualTo("https://cos.example.com/signed");
    }

    @Test
    void nonOwnerCannotGenerateSignedUrl() {
        CurrentUserContext.set(new CurrentUser(11L, "labeler", "labeler@example.com", Set.of(RoleCode.LABELER), 1));
        when(objectFileMapper.selectById(99L)).thenReturn(objectFile(99L, 10L));

        assertThatThrownBy(() -> fileService.generateSignedUrl(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403001);
    }

    @Test
    void missingFileReturnsBusinessError() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        when(objectFileMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> fileService.generateSignedUrl(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    private static ObjectFileEntity objectFile(Long fileId, Long ownerId) {
        ObjectFileEntity entity = new ObjectFileEntity();
        entity.setId(fileId);
        entity.setOwnerId(ownerId);
        entity.setBucketName("labelhub-test");
        entity.setObjectKey("uploads/dataset/file.jsonl");
        entity.setOriginalFilename("dataset.jsonl");
        entity.setContentType("application/x-ndjson");
        entity.setFileSize(10L);
        entity.setStorageProvider("COS");
        return entity;
    }
}
