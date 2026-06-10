package com.labelhub.modules.media.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.async.AsyncJobService;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.media.domain.DatasetItemMediaContextEntity;
import com.labelhub.modules.media.domain.MediaAssetEntity;
import com.labelhub.modules.media.domain.MediaProcessingJobEntity;
import com.labelhub.modules.media.dto.MediaProcessingJobResponse;
import com.labelhub.modules.media.mapper.DatasetItemMediaContextMapper;
import com.labelhub.modules.media.mapper.MediaAssetMapper;
import com.labelhub.modules.media.mapper.MediaDerivativeMapper;
import com.labelhub.modules.media.mapper.MediaProcessingJobMapper;
import com.labelhub.modules.storage.domain.ObjectFileEntity;
import com.labelhub.modules.storage.repository.ObjectFileMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaProcessingServiceTest {

    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final DatasetItemMapper datasetItemMapper = mock(DatasetItemMapper.class);
    private final ObjectFileMapper objectFileMapper = mock(ObjectFileMapper.class);
    private final MediaAssetMapper mediaAssetMapper = mock(MediaAssetMapper.class);
    private final MediaDerivativeMapper mediaDerivativeMapper = mock(MediaDerivativeMapper.class);
    private final DatasetItemMediaContextMapper contextMapper = mock(DatasetItemMediaContextMapper.class);
    private final MediaProcessingJobMapper jobMapper = mock(MediaProcessingJobMapper.class);
    private final AsyncJobService asyncJobService = mock(AsyncJobService.class);
    private final MediaProcessingService service = new MediaProcessingService(
            taskMapper,
            datasetItemMapper,
            objectFileMapper,
            mediaAssetMapper,
            mediaDerivativeMapper,
            contextMapper,
            jobMapper,
            asyncJobService,
            new ObjectMapper()
    );

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void refreshContextCreatesMediaAssetAndReadyImageContextForUploadedFile() {
        when(objectFileMapper.selectById(99L)).thenReturn(objectFile(99L, "image/png", 123L, "sha"));
        when(mediaAssetMapper.insert(any(MediaAssetEntity.class))).thenAnswer(invocation -> {
            MediaAssetEntity asset = invocation.getArgument(0);
            asset.setId(500L);
            return 1;
        });

        service.refreshContext(1L, 100L, "{\"media_type\":\"image\",\"media_file_id\":99}", 10L);

        ArgumentCaptor<MediaAssetEntity> assetCaptor = ArgumentCaptor.forClass(MediaAssetEntity.class);
        ArgumentCaptor<DatasetItemMediaContextEntity> contextCaptor = ArgumentCaptor.forClass(DatasetItemMediaContextEntity.class);
        verify(mediaAssetMapper).insert(assetCaptor.capture());
        verify(contextMapper).insert(contextCaptor.capture());
        assertThat(assetCaptor.getValue().getSourceFileId()).isEqualTo(99L);
        assertThat(assetCaptor.getValue().getStatus()).isEqualTo("READY");
        assertThat(contextCaptor.getValue().getProcessingStatus()).isEqualTo("READY");
        assertThat(contextCaptor.getValue().getContextJson()).contains("\"media_file_id\":99");
    }

    @Test
    void refreshContextMarksUploadedVideoReadyForDirectVideoModelInput() {
        when(objectFileMapper.selectById(99L)).thenReturn(objectFile(99L, "video/mp4", 123L, "sha"));
        when(mediaAssetMapper.insert(any(MediaAssetEntity.class))).thenAnswer(invocation -> {
            MediaAssetEntity asset = invocation.getArgument(0);
            asset.setId(500L);
            return 1;
        });

        service.refreshContext(1L, 100L, "{\"media_type\":\"video\",\"media_file_id\":99}", 10L);

        ArgumentCaptor<DatasetItemMediaContextEntity> contextCaptor = ArgumentCaptor.forClass(DatasetItemMediaContextEntity.class);
        verify(contextMapper).insert(contextCaptor.capture());
        assertThat(contextCaptor.getValue().getProcessingStatus()).isEqualTo("READY");
        assertThat(contextCaptor.getValue().getLimitationsJson()).isEqualTo("[]");
    }

    @Test
    void triggerProcessingCreatesPendingJobAndSubmitsAsyncWork() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        Task task = new Task();
        task.setId(1L);
        task.setOwnerId(10L);
        when(taskMapper.selectById(1L)).thenReturn(task);
        DatasetItem item = new DatasetItem();
        item.setId(100L);
        item.setTaskId(1L);
        item.setItemJson("{\"media_type\":\"image\",\"media_file_id\":99}");
        when(datasetItemMapper.selectById(100L)).thenReturn(item);
        when(jobMapper.insert(any(MediaProcessingJobEntity.class))).thenAnswer(invocation -> {
            MediaProcessingJobEntity job = invocation.getArgument(0);
            job.setId(700L);
            return 1;
        });

        MediaProcessingJobResponse response = service.triggerProcessing(100L);

        assertThat(response.jobId()).isEqualTo(700L);
        assertThat(response.status()).isEqualTo("PENDING");
        verify(asyncJobService).submit(any());
    }

    private static ObjectFileEntity objectFile(Long id, String contentType, Long fileSize, String checksum) {
        ObjectFileEntity entity = new ObjectFileEntity();
        entity.setId(id);
        entity.setBucketName("labelhub-test");
        entity.setObjectKey("uploads/media/file");
        entity.setOriginalFilename("file");
        entity.setContentType(contentType);
        entity.setFileSize(fileSize);
        entity.setChecksum(checksum);
        return entity;
    }
}
