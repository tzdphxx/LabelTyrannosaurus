package com.labelhub.modules.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.async.AsyncJobExecutor;
import com.labelhub.infrastructure.async.AsyncJobService;
import com.labelhub.infrastructure.storage.ObjectStorageService;
import com.labelhub.modules.dataset.domain.DatasetFileEntity;
import com.labelhub.modules.dataset.domain.DatasetImportJobEntity;
import com.labelhub.modules.dataset.domain.DatasetItemEntity;
import com.labelhub.modules.dataset.domain.DatasetType;
import com.labelhub.modules.dataset.dto.DatasetImportRequest;
import com.labelhub.modules.dataset.repository.DatasetFileMapper;
import com.labelhub.modules.dataset.repository.DatasetImportJobMapper;
import com.labelhub.modules.dataset.repository.DatasetItemChangeLogMapper;
import com.labelhub.modules.dataset.repository.DatasetItemMapper;
import com.labelhub.modules.dataset.service.DatasetImportService;
import com.labelhub.modules.dataset.service.ExcelDatasetParser;
import com.labelhub.modules.dataset.service.JsonDatasetParser;
import com.labelhub.modules.dataset.service.JsonlDatasetParser;
import com.labelhub.modules.storage.domain.ObjectFileEntity;
import com.labelhub.modules.storage.repository.ObjectFileMapper;
import com.labelhub.modules.storage.service.FileStorageProperties;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.repository.TaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetImportServiceTest {

    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final ObjectFileMapper objectFileMapper = mock(ObjectFileMapper.class);
    private final DatasetFileMapper datasetFileMapper = mock(DatasetFileMapper.class);
    private final DatasetImportJobMapper importJobMapper = mock(DatasetImportJobMapper.class);
    private final DatasetItemMapper datasetItemMapper = mock(DatasetItemMapper.class);
    private final DatasetItemChangeLogMapper changeLogMapper = mock(DatasetItemChangeLogMapper.class);
    private final ObjectStorageService objectStorageService = mock(ObjectStorageService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DatasetImportService service = new DatasetImportService(
            taskMapper,
            objectFileMapper,
            datasetFileMapper,
            importJobMapper,
            datasetItemMapper,
            changeLogMapper,
            objectStorageService,
            new FileStorageProperties("labelhub-test", 200L * 1024L * 1024L, Duration.ofMinutes(10)),
            new AsyncJobService(new AsyncJobExecutor(Runnable::run, List.of())),
            objectMapper,
            List.of(new JsonDatasetParser(objectMapper), new JsonlDatasetParser(objectMapper), new ExcelDatasetParser(objectMapper))
    );

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void appendImportProcessesValidJsonlRows() throws Exception {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(TaskStatus.DRAFT);
        stubSourceFile("qa_quality.jsonl");
        stubIds();
        when(objectStorageService.openReadStream("labelhub-test", "uploads/dataset/qa_quality.jsonl"))
                .thenReturn(new ByteArrayInputStream("""
                        {"externalId":"q1","question":"one"}
                        {"externalId":"q2","question":"two"}
                        """.getBytes(StandardCharsets.UTF_8)));
        when(datasetItemMapper.countActiveByTaskIdAndExternalId(eq(1L), any())).thenReturn(0);

        var response = service.createAppendImport(1L, new DatasetImportRequest(99L, DatasetType.QA_QUALITY));

        assertThat(response.jobId()).isEqualTo(300L);
        ArgumentCaptor<DatasetItemEntity> itemCaptor = ArgumentCaptor.forClass(DatasetItemEntity.class);
        verify(datasetItemMapper, org.mockito.Mockito.times(2)).insert(itemCaptor.capture());
        assertThat(itemCaptor.getAllValues()).extracting("externalId").containsExactly("q1", "q2");
        ArgumentCaptor<DatasetImportJobEntity> jobCaptor = ArgumentCaptor.forClass(DatasetImportJobEntity.class);
        verify(importJobMapper, org.mockito.Mockito.atLeastOnce()).updateById(jobCaptor.capture());
        assertThat(jobCaptor.getAllValues().get(jobCaptor.getAllValues().size() - 1).getStatus()).isEqualTo("SUCCESS");
        verify(objectStorageService, never()).upload(eq("labelhub-test"), org.mockito.Mockito.contains("error"), any(), any(), anyLong());
    }

    @Test
    void duplicateExternalIdCreatesErrorReportAndPartialSuccess() throws Exception {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(TaskStatus.DRAFT);
        stubSourceFile("qa_quality.jsonl");
        stubIds();
        when(objectStorageService.openReadStream("labelhub-test", "uploads/dataset/qa_quality.jsonl"))
                .thenReturn(new ByteArrayInputStream("""
                        {"externalId":"q1","question":"one"}
                        {"externalId":"q1","question":"duplicate"}
                        """.getBytes(StandardCharsets.UTF_8)));
        when(datasetItemMapper.countActiveByTaskIdAndExternalId(1L, "q1")).thenReturn(0, 1);
        when(objectStorageService.generatePresignedDownloadUrl(eq("labelhub-test"), any(), eq("dataset-import-300-errors.jsonl"), any()))
                .thenReturn(new URL("https://cos.example.com/errors"));

        service.createAppendImport(1L, new DatasetImportRequest(99L, DatasetType.QA_QUALITY));

        verify(datasetItemMapper, org.mockito.Mockito.times(1)).insert(any(DatasetItemEntity.class));
        verify(objectStorageService).upload(eq("labelhub-test"), org.mockito.Mockito.contains("dataset-import-300-errors.jsonl"),
                eq("application/x-ndjson"), any(), anyLong());
        ArgumentCaptor<DatasetImportJobEntity> jobCaptor = ArgumentCaptor.forClass(DatasetImportJobEntity.class);
        verify(importJobMapper, org.mockito.Mockito.atLeastOnce()).updateById(jobCaptor.capture());
        DatasetImportJobEntity finalJob = jobCaptor.getAllValues().get(jobCaptor.getAllValues().size() - 1);
        assertThat(finalJob.getStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(finalJob.getSuccessCount()).isEqualTo(1);
        assertThat(finalJob.getFailedCount()).isEqualTo(1);
        assertThat(finalJob.getErrorReportFileId()).isNotNull();
    }

    @Test
    void overwriteImportRequiresDraftTask() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(TaskStatus.PUBLISHED);
        stubSourceFile("qa_quality.jsonl");

        assertThatThrownBy(() -> service.createOverwriteImport(1L, new DatasetImportRequest(99L, DatasetType.QA_QUALITY)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409301);
    }

    private void stubTask(TaskStatus status) {
        TaskEntity task = new TaskEntity();
        task.setId(1L);
        task.setOwnerId(10L);
        task.setStatus(status);
        when(taskMapper.selectById(1L)).thenReturn(task);
    }

    private void stubSourceFile(String filename) {
        ObjectFileEntity file = new ObjectFileEntity();
        file.setId(99L);
        file.setOwnerId(10L);
        file.setBucketName("labelhub-test");
        file.setObjectKey("uploads/dataset/" + filename);
        file.setOriginalFilename(filename);
        file.setContentType("application/x-ndjson");
        file.setFileSize(100L);
        file.setStorageProvider("COS");
        when(objectFileMapper.selectById(99L)).thenReturn(file);
    }

    private void stubIds() {
        AtomicLong ids = new AtomicLong(200L);
        when(datasetFileMapper.insert(any(DatasetFileEntity.class))).thenAnswer(invocation -> {
            DatasetFileEntity entity = invocation.getArgument(0);
            entity.setId(ids.getAndIncrement());
            return 1;
        });
        when(importJobMapper.insert(any(DatasetImportJobEntity.class))).thenAnswer(invocation -> {
            DatasetImportJobEntity entity = invocation.getArgument(0);
            entity.setId(300L);
            return 1;
        });
        when(objectFileMapper.insert(any(ObjectFileEntity.class))).thenAnswer(invocation -> {
            ObjectFileEntity entity = invocation.getArgument(0);
            entity.setId(ids.getAndIncrement());
            return 1;
        });
    }
}
