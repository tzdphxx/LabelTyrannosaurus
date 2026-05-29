package com.labelhub.modules.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.async.AsyncJobExecutor;
import com.labelhub.infrastructure.async.AsyncJobService;
import com.labelhub.infrastructure.storage.ObjectStorageService;
import com.labelhub.modules.export.domain.ExportFormat;
import com.labelhub.modules.export.domain.ExportJobEntity;
import com.labelhub.modules.export.dto.CreateExportRequest;
import com.labelhub.modules.export.dto.ExportFieldMapping;
import com.labelhub.modules.export.repository.ExportJobMapper;
import com.labelhub.modules.export.service.ExportJobService;
import com.labelhub.modules.export.service.JsonlExportFileWriter;
import com.labelhub.modules.storage.domain.ObjectFileEntity;
import com.labelhub.modules.storage.repository.ObjectFileMapper;
import com.labelhub.modules.storage.service.FileStorageProperties;
import com.labelhub.modules.submission.dto.ExportPageRequest;
import com.labelhub.modules.submission.dto.ExportableSubmissionSnapshot;
import com.labelhub.modules.submission.service.SubmissionExportQueryService;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.repository.TaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportJobServiceTest {

    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final ExportJobMapper exportJobMapper = mock(ExportJobMapper.class);
    private final ObjectFileMapper objectFileMapper = mock(ObjectFileMapper.class);
    private final ObjectStorageService objectStorageService = mock(ObjectStorageService.class);
    private final SubmissionExportQueryService submissionExportQueryService = mock(SubmissionExportQueryService.class);
    private final AuditAppender auditAppender = mock(AuditAppender.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExportJobService exportJobService = new ExportJobService(
            taskMapper,
            exportJobMapper,
            objectFileMapper,
            objectStorageService,
            new FileStorageProperties("labelhub-test", 200L * 1024L * 1024L, Duration.ofMinutes(10)),
            new AsyncJobService(new AsyncJobExecutor(Runnable::run, List.of())),
            submissionExportQueryService,
            auditAppender,
            objectMapper,
            List.of(new JsonlExportFileWriter(objectMapper))
    );

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void createExportRunsAsyncJobAndStoresResultFile() throws Exception {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);
        stubJobAndFileIds();
        when(submissionExportQueryService.queryExportableGoldenSubmissions(eq(1L), any(ExportPageRequest.class)))
                .thenReturn(List.of(snapshot()), List.of());
        when(objectStorageService.generatePresignedDownloadUrl(eq("labelhub-test"), any(), eq("task-1-export-500.jsonl"), any()))
                .thenReturn(new URL("https://cos.example.com/export.jsonl"));

        var response = exportJobService.createExport(1L, request(), "trace-1");

        assertThat(response.exportJobId()).isEqualTo(500L);
        ArgumentCaptor<ExportJobEntity> jobCaptor = ArgumentCaptor.forClass(ExportJobEntity.class);
        verify(exportJobMapper).insert(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(jobCaptor.getValue().getTraceId()).isEqualTo("trace-1");
        ArgumentCaptor<ExportJobEntity> updateCaptor = ArgumentCaptor.forClass(ExportJobEntity.class);
        verify(exportJobMapper, org.mockito.Mockito.atLeastOnce()).updateById(updateCaptor.capture());
        ExportJobEntity finalJob = updateCaptor.getAllValues().get(updateCaptor.getAllValues().size() - 1);
        assertThat(finalJob.getStatus()).isEqualTo("SUCCESS");
        assertThat(finalJob.getResultFileId()).isEqualTo(900L);
        assertThat(finalJob.getDownloadUrl()).isEqualTo("https://cos.example.com/export.jsonl");
        verify(objectStorageService).upload(eq("labelhub-test"), org.mockito.Mockito.contains("exports/task-1/"),
                eq("application/x-ndjson"), any(InputStream.class), anyLong());
    }

    @Test
    void nonOwnerCannotCreateExport() {
        CurrentUserContext.set(new CurrentUser(11L, "owner2", "owner2@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);

        assertThatThrownBy(() -> exportJobService.createExport(1L, request(), "trace-2"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403001);
        verify(exportJobMapper, never()).insert(any(ExportJobEntity.class));
    }

    @Test
    void failedUploadMarksJobFailedWithTraceId() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);
        stubJobAndFileIds();
        when(submissionExportQueryService.queryExportableGoldenSubmissions(eq(1L), any(ExportPageRequest.class)))
                .thenReturn(List.of(snapshot()), List.of());
        doThrow(new IllegalStateException("storage unavailable"))
                .when(objectStorageService)
                .upload(eq("labelhub-test"), any(), eq("application/x-ndjson"), any(InputStream.class), anyLong());

        exportJobService.createExport(1L, request(), "trace-3");

        ArgumentCaptor<ExportJobEntity> updateCaptor = ArgumentCaptor.forClass(ExportJobEntity.class);
        verify(exportJobMapper, org.mockito.Mockito.atLeastOnce()).updateById(updateCaptor.capture());
        ExportJobEntity finalJob = updateCaptor.getAllValues().get(updateCaptor.getAllValues().size() - 1);
        assertThat(finalJob.getStatus()).isEqualTo("FAILED");
        assertThat(finalJob.getTraceId()).isEqualTo("trace-3");
        assertThat(finalJob.getErrorMessage()).contains("storage unavailable");
        verify(objectFileMapper, never()).insert(any(ObjectFileEntity.class));
    }

    private CreateExportRequest request() {
        return new CreateExportRequest(
                ExportFormat.JSONL,
                true,
                false,
                false,
                false,
                List.of(new ExportFieldMapping("$.submissionId", "submission_id", null, true))
        );
    }

    private void stubTask(Long ownerId) {
        TaskEntity task = new TaskEntity();
        task.setId(1L);
        task.setOwnerId(ownerId);
        task.setStatus(TaskStatus.PUBLISHED);
        when(taskMapper.selectById(1L)).thenReturn(task);
    }

    private void stubJobAndFileIds() {
        when(exportJobMapper.insert(any(ExportJobEntity.class))).thenAnswer(invocation -> {
            ExportJobEntity entity = invocation.getArgument(0);
            entity.setId(500L);
            return 1;
        });
        when(objectFileMapper.insert(any(ObjectFileEntity.class))).thenAnswer(invocation -> {
            ObjectFileEntity entity = invocation.getArgument(0);
            entity.setId(900L);
            return 1;
        });
    }

    private ExportableSubmissionSnapshot snapshot() {
        return new ExportableSubmissionSnapshot(
                200L,
                11L,
                objectMapper.valueToTree(Map.of("question", "What is A?")),
                objectMapper.valueToTree(Map.of("answer", "A")),
                objectMapper.valueToTree(Map.of("decision", "PASS")),
                List.of(),
                null,
                null,
                LocalDateTime.parse("2026-05-01T10:00:00")
        );
    }
}
