package com.labelhub.modules.export.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.async.AsyncJobCommand;
import com.labelhub.infrastructure.async.AsyncJobService;
import com.labelhub.infrastructure.async.AsyncJobType;
import com.labelhub.infrastructure.storage.ObjectStorageService;
import com.labelhub.modules.export.domain.ExportFormat;
import com.labelhub.modules.export.domain.ExportJobEntity;
import com.labelhub.modules.export.domain.ExportJobStatus;
import com.labelhub.modules.export.dto.CreateExportRequest;
import com.labelhub.modules.export.dto.ExportFieldMapping;
import com.labelhub.modules.export.dto.ExportJobPageResponse;
import com.labelhub.modules.export.dto.ExportJobResponse;
import com.labelhub.modules.export.repository.ExportJobMapper;
import com.labelhub.modules.storage.domain.ObjectFileEntity;
import com.labelhub.modules.storage.repository.ObjectFileMapper;
import com.labelhub.modules.storage.service.FileStorageProperties;
import com.labelhub.modules.submission.dto.ExportPageRequest;
import com.labelhub.modules.submission.dto.ExportableSubmissionSnapshot;
import com.labelhub.modules.submission.service.SubmissionExportQueryService;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.repository.TaskRepositoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导出任务应用服务。
 *
 * <p>该服务负责导出任务编排、异步文件生成、对象存储落盘和历史查询；数据快照必须通过
 * BE-A 的只读导出查询服务获取，禁止直接推进 submission 或 review 状态。</p>
 */
@Service
public class ExportJobService {

    private static final Logger log = LoggerFactory.getLogger(ExportJobService.class);
    private static final int DEFAULT_PAGE_SIZE = 500;

    private final TaskRepositoryMapper taskMapper;
    private final ExportJobMapper exportJobMapper;
    private final ObjectFileMapper objectFileMapper;
    private final ObjectStorageService objectStorageService;
    private final FileStorageProperties storageProperties;
    private final AsyncJobService asyncJobService;
    private final SubmissionExportQueryService submissionExportQueryService;
    private final AuditAppender auditAppender;
    private final ObjectMapper objectMapper;
    private final Map<String, ExportFileWriter> writerMap;

    public ExportJobService(TaskRepositoryMapper taskMapper,
                            ExportJobMapper exportJobMapper,
                            ObjectFileMapper objectFileMapper,
                            ObjectStorageService objectStorageService,
                            FileStorageProperties storageProperties,
                            AsyncJobService asyncJobService,
                            SubmissionExportQueryService submissionExportQueryService,
                            AuditAppender auditAppender,
                            ObjectMapper objectMapper,
                            List<ExportFileWriter> writers) {
        this.taskMapper = taskMapper;
        this.exportJobMapper = exportJobMapper;
        this.objectFileMapper = objectFileMapper;
        this.objectStorageService = objectStorageService;
        this.storageProperties = storageProperties;
        this.asyncJobService = asyncJobService;
        this.submissionExportQueryService = submissionExportQueryService;
        this.auditAppender = auditAppender;
        this.objectMapper = objectMapper;
        this.writerMap = writers.stream().collect(java.util.stream.Collectors.toMap(writer -> writer.format().name(), w -> w));
    }

    /**
     * 创建导出任务并异步执行。
     */
    @Transactional
    public ExportJobResponse createExport(Long taskId, CreateExportRequest request, String traceId) {
        TaskEntity task = requireOwnedTask(taskId);
        ExportFormat format = request.exportFormat() == null ? ExportFormat.JSONL : request.exportFormat();
        ExportFileWriter writer = requireWriter(format);
        CurrentUser actor = CurrentUserContext.requireCurrentUser();

        ExportJobEntity job = new ExportJobEntity();
        job.setTaskId(task.getId());
        job.setRequestedBy(actor.userId());
        job.setExportFormat(format.name());
        job.setStatus(ExportJobStatus.PENDING.name());
        job.setIncludeAiReview(normalizeFlag(request.includeAiReview()));
        job.setIncludeAuditTrail(normalizeFlag(request.includeAuditTrail()));
        job.setIncludeReviewComment(normalizeFlag(request.includeReviewComment()));
        job.setIncludeLabelerInfo(normalizeFlag(request.includeLabelerInfo()));
        job.setFieldMappingJson(writeJson(request.fieldMappings()));
        job.setTraceId(traceId);
        exportJobMapper.insert(job);
        appendAudit("EXPORT_CREATED", actor.userId(), job, traceId);

        asyncJobService.submit(new AsyncJobCommand(
                AsyncJobType.EXPORT,
                job.getId(),
                traceId,
                () -> runExport(job.getId(), task.getId(), actor.userId(), writer, request, traceId)
        ));
        return toResponse(job);
    }

    /**
     * 查询任务下的导出历史。
     */
    public ExportJobPageResponse listExports(Long taskId, Integer page, Integer pageSize) {
        requireOwnedTask(taskId);
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        long total = exportJobMapper.countByTask(taskId);
        List<ExportJobResponse> items = exportJobMapper.selectPageByTask(taskId, normalizedPageSize, (normalizedPage - 1) * normalizedPageSize)
                .stream()
                .map(this::toResponse)
                .toList();
        return new ExportJobPageResponse(items, normalizedPage, normalizedPageSize, total);
    }

    /**
     * 查询导出任务详情。
     */
    public ExportJobResponse getExportJob(Long taskId, Long exportJobId) {
        requireOwnedTask(taskId);
        ExportJobEntity job = exportJobMapper.selectByTaskAndJob(taskId, exportJobId);
        if (job == null) {
            throw new BusinessException(400102, "Export job not found");
        }
        if (ExportJobStatus.SUCCESS.name().equals(job.getStatus()) && job.getResultFileId() != null) {
            refreshDownloadUrl(job);
        }
        return toResponse(job);
    }

    private void runExport(Long exportJobId,
                           Long taskId,
                           Long actorId,
                           ExportFileWriter writer,
                           CreateExportRequest request,
                           String traceId) {
        ExportJobEntity running = new ExportJobEntity();
        running.setId(exportJobId);
        running.setStatus(ExportJobStatus.RUNNING.name());
        running.setStartedAt(java.time.LocalDateTime.now());
        running.setTraceId(traceId);
        exportJobMapper.updateById(running);

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("labelhub-export-", "." + writer.format().extension());
            try (OutputStream outputStream = Files.newOutputStream(tempFile);
                 ExportFileWriter.ExportFileWriteSession session = writer.open(outputStream, request.fieldMappings())) {
                Long cursor = null;
                while (true) {
                    // 只读依赖 BE-A 的金标导出快照，不直接读取或修改 submission 状态字段。
                    List<ExportableSubmissionSnapshot> page = submissionExportQueryService.queryExportableGoldenSubmissions(
                            taskId,
                            new ExportPageRequest(cursor, DEFAULT_PAGE_SIZE,
                                    normalizeFlag(request.includeAiReview()),
                                    normalizeFlag(request.includeAuditTrail()),
                                    normalizeFlag(request.includeReviewComment()),
                                    normalizeFlag(request.includeLabelerInfo()))
                    );
                    if (page.isEmpty()) {
                        break;
                    }
                    for (ExportableSubmissionSnapshot snapshot : page) {
                        session.writeRow(snapshot);
                        cursor = snapshot.submissionId();
                    }
                    if (page.size() < DEFAULT_PAGE_SIZE) {
                        break;
                    }
                }
            }

            long size = Files.size(tempFile);
            String objectKey = buildObjectKey(taskId, exportJobId, writer.format());
            try (InputStream inputStream = Files.newInputStream(tempFile)) {
                objectStorageService.upload(storageProperties.bucket(), objectKey, writer.contentType(), inputStream, size);
            }

            ObjectFileEntity file = new ObjectFileEntity();
            file.setOwnerId(actorId);
            file.setBucketName(storageProperties.bucket());
            file.setObjectKey(objectKey);
            file.setOriginalFilename(buildFilename(taskId, exportJobId, writer.format()));
            file.setContentType(writer.contentType());
            file.setFileSize(size);
            file.setStorageProvider("COS");
            objectFileMapper.insert(file);

            URL downloadUrl = objectStorageService.generatePresignedDownloadUrl(
                    storageProperties.bucket(),
                    objectKey,
                    file.getOriginalFilename(),
                    Instant.now().plus(storageProperties.signedUrlTtl())
            );

            ExportJobEntity success = new ExportJobEntity();
            success.setId(exportJobId);
            success.setStatus(ExportJobStatus.SUCCESS.name());
            success.setResultFileId(file.getId());
            success.setDownloadUrl(downloadUrl.toString());
            success.setFinishedAt(java.time.LocalDateTime.now());
            success.setTraceId(traceId);
            exportJobMapper.updateById(success);
            appendAudit("EXPORT_FINISHED", actorId, success, traceId);
        } catch (Exception ex) {
            ExportJobEntity failed = new ExportJobEntity();
            failed.setId(exportJobId);
            failed.setStatus(ExportJobStatus.FAILED.name());
            failed.setErrorMessage(ex.getMessage());
            failed.setTraceId(traceId);
            failed.setFinishedAt(java.time.LocalDateTime.now());
            exportJobMapper.updateById(failed);
            appendAudit("EXPORT_FAILED", actorId, failed, traceId);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception cleanupFailure) {
                    log.warn("Failed to delete export temp file {}", tempFile, cleanupFailure);
                }
            }
        }
    }

    private TaskEntity requireOwnedTask(Long taskId) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(400102, "Task not found");
        }
        if (!currentUser.roles().contains(RoleCode.ADMIN) && !currentUser.userId().equals(task.getOwnerId())) {
            throw new BusinessException(403001, "Forbidden");
        }
        if (task.getStatus() == TaskStatus.DRAFT) {
            // 导出依赖 BE-A 已生成的金标结果，草稿任务没有稳定导出范围。
            throw new BusinessException(400101, "Task is not exportable");
        }
        return task;
    }

    private ExportFileWriter requireWriter(ExportFormat format) {
        ExportFileWriter writer = writerMap.get(format.name());
        if (writer == null) {
            throw new BusinessException(400102, "Unsupported export format");
        }
        return writer;
    }

    private boolean normalizeFlag(Boolean value) {
        return value != null && value;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500001, "Export job serialization failed");
        }
    }

    private String buildObjectKey(Long taskId, Long exportJobId, ExportFormat format) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return "exports/task-%d/%04d/%02d/%02d/export-%d.%s".formatted(
                taskId,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                exportJobId,
                format.extension()
        );
    }

    private String buildFilename(Long taskId, Long exportJobId, ExportFormat format) {
        return "task-%d-export-%d.%s".formatted(taskId, exportJobId, format.extension());
    }

    private void refreshDownloadUrl(ExportJobEntity job) {
        ObjectFileEntity file = objectFileMapper.selectById(job.getResultFileId());
        if (file == null) {
            throw new BusinessException(400102, "Export file not found");
        }
        URL downloadUrl = objectStorageService.generatePresignedDownloadUrl(
                file.getBucketName(),
                file.getObjectKey(),
                file.getOriginalFilename(),
                Instant.now().plus(storageProperties.signedUrlTtl())
        );
        job.setDownloadUrl(downloadUrl.toString());
        exportJobMapper.updateById(job);
    }

    private ExportJobResponse toResponse(ExportJobEntity job) {
        return new ExportJobResponse(
                job.getId(),
                job.getTaskId(),
                job.getExportFormat(),
                job.getStatus(),
                job.getIncludeAiReview(),
                job.getIncludeAuditTrail(),
                job.getIncludeReviewComment(),
                job.getIncludeLabelerInfo(),
                job.getFieldMappingJson(),
                job.getResultFileId(),
                job.getDownloadUrl(),
                job.getErrorMessage(),
                job.getTraceId(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getCreatedAt()
        );
    }

    private void appendAudit(String action, Long actorId, ExportJobEntity job, String traceId) {
        Map<String, Object> after = new LinkedHashMap<>();
        if (job.getTaskId() != null) {
            after.put("taskId", job.getTaskId());
        }
        if (job.getExportFormat() != null) {
            after.put("exportFormat", job.getExportFormat());
        }
        if (job.getStatus() != null) {
            after.put("status", job.getStatus());
        }
        auditAppender.append(new AuditCommand(
                "USER",
                actorId,
                "EXPORT_JOB",
                job.getId(),
                action,
                Map.of(),
                after,
                traceId,
                null
        ));
    }
}
