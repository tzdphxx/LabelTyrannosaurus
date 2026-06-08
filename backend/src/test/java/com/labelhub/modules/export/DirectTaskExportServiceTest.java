package com.labelhub.modules.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.export.dto.DirectTaskExportRequest;
import com.labelhub.modules.export.dto.GeneratedTaskExportFile;
import com.labelhub.modules.export.dto.TaskExportFormat;
import com.labelhub.modules.export.service.DirectTaskExportService;
import com.labelhub.modules.export.service.TaskExportFileWriter;
import com.labelhub.modules.storage.dto.FileUploadResponse;
import com.labelhub.modules.storage.service.FileService;
import com.labelhub.modules.submission.repository.ApprovedSubmissionExportRecord;
import com.labelhub.modules.submission.repository.SubmissionExportMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DirectTaskExportServiceTest {

    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final SubmissionExportMapper submissionExportMapper = mock(SubmissionExportMapper.class);
    private final FileService fileService = mock(FileService.class);
    private final TaskExportFileWriter writer = mock(TaskExportFileWriter.class);
    private final DirectTaskExportService service = new DirectTaskExportService(
            taskMapper,
            submissionExportMapper,
            fileService,
            new ObjectMapper(),
            writer
    );

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void ownerExportsApprovedSubmissionsAndUploadsGeneratedFile() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);
        when(submissionExportMapper.selectApprovedSubmissionsForExport(1L, null, 500, true, true))
                .thenReturn(List.of(record(200L, 10L, 1)), List.of());
        when(writer.write(eq(TaskExportFormat.JSONL), any())).thenReturn(new GeneratedTaskExportFile(
                "application/x-ndjson",
                "jsonl",
                "{\"submissionId\":200}\n".getBytes(StandardCharsets.UTF_8)
        ));
        when(fileService.uploadGenerated(any(), eq("task-1-approved-submissions.jsonl"),
                eq("application/x-ndjson"), eq(10L), eq("export"))).thenReturn(uploadResponse());

        var response = service.exportDirect(1L, new DirectTaskExportRequest(null));

        assertThat(response.fileId()).isEqualTo(900L);
        assertThat(response.filename()).isEqualTo("task-1-approved-submissions.jsonl");
        assertThat(response.downloadUrl()).isEqualTo("https://cos.example.com/export.jsonl");
        assertThat(response.exportedCount()).isEqualTo(1);
        ArgumentCaptor<List> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(writer).write(eq(TaskExportFormat.JSONL), rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).hasSize(1);
        verify(fileService).uploadGenerated(any(), eq("task-1-approved-submissions.jsonl"),
                eq("application/x-ndjson"), eq(10L), eq("export"));
    }

    @Test
    void adminCanExportOtherOwnersTask() {
        CurrentUserContext.set(new CurrentUser(99L, "admin", "admin@example.com", Set.of(RoleCode.ADMIN), 1));
        stubTask(10L);
        when(submissionExportMapper.selectApprovedSubmissionsForExport(1L, null, 500, true, true))
                .thenReturn(List.of(), List.of());
        when(writer.write(eq(TaskExportFormat.CSV), any())).thenReturn(new GeneratedTaskExportFile(
                "text/csv",
                "csv",
                "taskId\n".getBytes(StandardCharsets.UTF_8)
        ));
        when(fileService.uploadGenerated(any(), eq("task-1-approved-submissions.csv"),
                eq("text/csv"), eq(10L), eq("export"))).thenReturn(uploadResponse());

        var response = service.exportDirect(1L, new DirectTaskExportRequest("CSV"));

        assertThat(response.exportedCount()).isZero();
        verify(fileService).uploadGenerated(any(), eq("task-1-approved-submissions.csv"),
                eq("text/csv"), eq(10L), eq("export"));
    }

    @Test
    void ownerCannotExportOtherOwnersTask() {
        CurrentUserContext.set(new CurrentUser(11L, "owner2", "owner2@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);

        assertThatThrownBy(() -> service.exportDirect(1L, new DirectTaskExportRequest("JSONL")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403001);
        verify(submissionExportMapper, never()).selectApprovedSubmissionsForExport(any(), any(), anyInt(), anyBoolean(), anyBoolean());
    }

    @Test
    void invalidFormatReturnsParameterErrorBeforeQueryingSubmissions() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);

        assertThatThrownBy(() -> service.exportDirect(1L, new DirectTaskExportRequest("PDF")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
        verify(submissionExportMapper, never()).selectApprovedSubmissionsForExport(any(), any(), anyInt(), anyBoolean(), anyBoolean());
    }

    @Test
    void approvedExportQueryDoesNotUseGoldenCondition() throws Exception {
        Method method = SubmissionExportMapper.class.getMethod(
                "selectApprovedSubmissionsForExport",
                Long.class,
                Long.class,
                int.class,
                boolean.class,
                boolean.class
        );
        String sql = String.join("\n", method.getAnnotation(Select.class).value()).toLowerCase();

        assertThat(sql).contains("s.task_id = #{taskid}");
        assertThat(sql).contains("s.status = 'approved'");
        assertThat(sql).doesNotContain("is_golden");
        assertThat(sql).doesNotContain("isgolden");
    }

    private void stubTask(Long ownerId) {
        Task task = new Task();
        task.setId(1L);
        task.setOwnerId(ownerId);
        when(taskMapper.selectById(1L)).thenReturn(task);
    }

    private ApprovedSubmissionExportRecord record(Long submissionId, Long labelerId, Integer versionNo) {
        return new ApprovedSubmissionExportRecord(
                1L,
                submissionId,
                20L,
                labelerId,
                versionNo,
                LocalDateTime.parse("2026-06-01T10:00:00"),
                "{\"text\":\"raw\"}",
                "{\"label\":\"ok\"}",
                "{\"decision\":\"PASS\"}",
                "looks good"
        );
    }

    private FileUploadResponse uploadResponse() {
        return new FileUploadResponse(
                900L,
                "task-1-approved-submissions.jsonl",
                "application/x-ndjson",
                21L,
                "uploads/export/file.jsonl",
                "checksum-1",
                "https://cos.example.com/export.jsonl"
        );
    }
}
