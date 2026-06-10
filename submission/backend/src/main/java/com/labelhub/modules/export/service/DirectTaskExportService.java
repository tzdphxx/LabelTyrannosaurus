package com.labelhub.modules.export.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.export.dto.DirectTaskExportRequest;
import com.labelhub.modules.export.dto.DirectTaskExportResponse;
import com.labelhub.modules.export.dto.GeneratedTaskExportFile;
import com.labelhub.modules.export.dto.TaskExportFormat;
import com.labelhub.modules.export.dto.TaskExportRow;
import com.labelhub.modules.storage.dto.FileUploadResponse;
import com.labelhub.modules.storage.service.FileService;
import com.labelhub.modules.submission.repository.ApprovedSubmissionExportRecord;
import com.labelhub.modules.submission.repository.SubmissionExportMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class DirectTaskExportService {

    private static final int PAGE_SIZE = 500;

    private final TaskMapper taskMapper;
    private final SubmissionExportMapper submissionExportMapper;
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final TaskExportFileWriter writer;

    public DirectTaskExportService(TaskMapper taskMapper,
                                   SubmissionExportMapper submissionExportMapper,
                                   FileService fileService,
                                   ObjectMapper objectMapper,
                                   TaskExportFileWriter writer) {
        this.taskMapper = taskMapper;
        this.submissionExportMapper = submissionExportMapper;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
        this.writer = writer;
    }

    @Transactional
    public DirectTaskExportResponse exportDirect(Long taskId, DirectTaskExportRequest request) {
        CurrentUser actor = CurrentUserContext.requireAnyRole(java.util.Set.of(RoleCode.OWNER, RoleCode.ADMIN));
        Task task = requireExportableTask(taskId, actor);
        TaskExportFormat format = TaskExportFormat.parseOrDefault(request == null ? null : request.format());

        List<TaskExportRow> rows = new ArrayList<>();
        Long cursor = null;
        while (true) {
            List<ApprovedSubmissionExportRecord> page = submissionExportMapper.selectApprovedSubmissionsForExport(
                    taskId,
                    cursor,
                    PAGE_SIZE,
                    true,
                    true
            );
            if (page.isEmpty()) {
                break;
            }
            for (ApprovedSubmissionExportRecord record : page) {
                rows.add(toRow(record));
                cursor = record.submissionId();
            }
            if (page.size() < PAGE_SIZE) {
                break;
            }
        }

        GeneratedTaskExportFile file = writer.write(format, rows);
        String filename = "task-%d-approved-submissions.%s".formatted(taskId, file.extension());
        FileUploadResponse upload = fileService.uploadGenerated(
                file.bytes(),
                filename,
                file.contentType(),
                task.getOwnerId(),
                "export"
        );
        return new DirectTaskExportResponse(
                upload.fileId(),
                upload.originalFilename(),
                upload.contentType(),
                upload.fileSize(),
                upload.checksum(),
                upload.downloadUrl(),
                rows.size()
        );
    }

    private Task requireExportableTask(Long taskId, CurrentUser actor) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(400102, "Task not found");
        }
        if (!actor.roles().contains(RoleCode.ADMIN) && !actor.userId().equals(task.getOwnerId())) {
            throw new BusinessException(403001, "Current user cannot export this task");
        }
        return task;
    }

    private TaskExportRow toRow(ApprovedSubmissionExportRecord record) {
        return new TaskExportRow(
                record.taskId(),
                record.submissionId(),
                record.datasetItemId(),
                record.labelerId(),
                record.versionNo(),
                record.submittedAt(),
                readJson(record.itemJson()),
                readJson(record.answerJson()),
                readJson(record.aiReviewJson()),
                record.reviewComment()
        );
    }

    private JsonNode readJson(String json) {
        if (!StringUtils.hasText(json)) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new BusinessException(500001, "Approved submission export JSON is invalid");
        }
    }
}
