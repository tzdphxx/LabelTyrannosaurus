package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.async.AsyncJobCommand;
import com.labelhub.infrastructure.async.AsyncJobService;
import com.labelhub.infrastructure.async.AsyncJobType;
import com.labelhub.infrastructure.storage.ObjectStorageService;
import com.labelhub.modules.dataset.domain.DatasetFileEntity;
import com.labelhub.modules.dataset.domain.DatasetFileFormat;
import com.labelhub.modules.dataset.domain.DatasetImportJobEntity;
import com.labelhub.modules.dataset.domain.DatasetImportMode;
import com.labelhub.modules.dataset.domain.DatasetImportStatus;
import com.labelhub.modules.dataset.domain.DatasetItemChangeLogEntity;
import com.labelhub.modules.dataset.domain.DatasetItemEntity;
import com.labelhub.modules.dataset.dto.DatasetImportJobResponse;
import com.labelhub.modules.dataset.dto.DatasetImportRequest;
import com.labelhub.modules.dataset.repository.DatasetFileMapper;
import com.labelhub.modules.dataset.repository.DatasetImportJobMapper;
import com.labelhub.modules.dataset.repository.DatasetItemChangeLogMapper;
import com.labelhub.modules.dataset.repository.DatasetItemMapper;
import com.labelhub.modules.storage.domain.ObjectFileEntity;
import com.labelhub.modules.storage.repository.ObjectFileMapper;
import com.labelhub.modules.storage.service.FileStorageProperties;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.repository.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据集导入应用服务。
 *
 * <p>本服务负责把已上传到对象存储的源文件解析为标准题目数据，并维护
 * {@code dataset_files}、{@code dataset_import_jobs}、{@code dataset_items}
 * 和错误报告文件之间的一致关系。导入过程允许单行失败，失败明细会汇总为
 * JSONL 错误报告，避免一条脏数据阻断整批导入。</p>
 */
@Service
public class DatasetImportService {

    private final TaskMapper taskMapper;
    private final ObjectFileMapper objectFileMapper;
    private final DatasetFileMapper datasetFileMapper;
    private final DatasetImportJobMapper importJobMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final DatasetItemChangeLogMapper changeLogMapper;
    private final ObjectStorageService objectStorageService;
    private final FileStorageProperties storageProperties;
    private final AsyncJobService asyncJobService;
    private final ObjectMapper objectMapper;
    private final Map<DatasetFileFormat, DatasetParser> parsers;

    public DatasetImportService(TaskMapper taskMapper,
                                ObjectFileMapper objectFileMapper,
                                DatasetFileMapper datasetFileMapper,
                                DatasetImportJobMapper importJobMapper,
                                DatasetItemMapper datasetItemMapper,
                                DatasetItemChangeLogMapper changeLogMapper,
                                ObjectStorageService objectStorageService,
                                FileStorageProperties storageProperties,
                                AsyncJobService asyncJobService,
                                ObjectMapper objectMapper,
                                List<DatasetParser> parsers) {
        this.taskMapper = taskMapper;
        this.objectFileMapper = objectFileMapper;
        this.datasetFileMapper = datasetFileMapper;
        this.importJobMapper = importJobMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.changeLogMapper = changeLogMapper;
        this.objectStorageService = objectStorageService;
        this.storageProperties = storageProperties;
        this.asyncJobService = asyncJobService;
        this.objectMapper = objectMapper;
        this.parsers = parsers.stream().collect(Collectors.toMap(DatasetParser::format, Function.identity()));
    }

    /**
     * 创建追加导入任务。
     *
     * <p>追加导入只新增不存在的 {@code externalId}，重复题目会作为行级失败记录。</p>
     */
    @Transactional
    public DatasetImportJobResponse createAppendImport(Long taskId, DatasetImportRequest request) {
        return createImport(taskId, request, DatasetImportMode.APPEND);
    }

    /**
     * 创建覆盖导入任务。
     *
     * <p>覆盖导入会软删除当前任务下已有题目，只允许在 DRAFT 状态下执行。</p>
     */
    @Transactional
    public DatasetImportJobResponse createOverwriteImport(Long taskId, DatasetImportRequest request) {
        return createImport(taskId, request, DatasetImportMode.OVERWRITE);
    }

    /**
     * 查询导入任务详情，并在存在错误报告时补充签名下载地址。
     */
    public DatasetImportJobResponse getImportJob(Long taskId, Long jobId) {
        TaskEntity task = requireWritableTask(taskId);
        DatasetImportJobEntity job = importJobMapper.selectByTaskAndJob(task.getId(), jobId);
        if (job == null) {
            throw new BusinessException(400102, "Import job not found");
        }
        return toResponse(job);
    }

    private DatasetImportJobResponse createImport(Long taskId, DatasetImportRequest request, DatasetImportMode mode) {
        TaskEntity task = requireWritableTask(taskId);
        if (mode == DatasetImportMode.OVERWRITE && task.getStatus() != TaskStatus.DRAFT) {
            throw new BusinessException(409301, "Overwrite import only allowed for draft task");
        }
        ObjectFileEntity sourceFile = requireSourceFile(request.fileId());
        DatasetFileFormat format = resolveFormat(sourceFile);
        DatasetParser parser = parsers.get(format);
        if (parser == null || format == DatasetFileFormat.CSV) {
            throw new BusinessException(400102, "Unsupported dataset file format");
        }

        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        // 源文件和导入任务先落库，后台任务执行失败时仍可查询到失败状态。
        DatasetFileEntity datasetFile = new DatasetFileEntity();
        datasetFile.setTaskId(task.getId());
        datasetFile.setFileId(sourceFile.getId());
        datasetFile.setFileFormat(format.name());
        datasetFile.setCreatedBy(currentUser.userId());
        datasetFileMapper.insert(datasetFile);

        DatasetImportJobEntity job = new DatasetImportJobEntity();
        job.setTaskId(task.getId());
        job.setDatasetFileId(datasetFile.getId());
        job.setStatus(DatasetImportStatus.PENDING.name());
        job.setImportMode(mode.name());
        job.setTotalCount(0);
        job.setSuccessCount(0);
        job.setFailedCount(0);
        job.setCreatedBy(currentUser.userId());
        importJobMapper.insert(job);

        asyncJobService.submit(new AsyncJobCommand(
                AsyncJobType.DATASET_IMPORT,
                job.getId(),
                null,
                () -> runImport(job, sourceFile, request, mode, parser, currentUser.userId())
        ));
        return toResponse(job);
    }

    private TaskEntity requireWritableTask(Long taskId) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(400102, "Task not found");
        }
        if (!currentUser.roles().contains(RoleCode.ADMIN) && !currentUser.userId().equals(task.getOwnerId())) {
            throw new BusinessException(403001, "Forbidden");
        }
        return task;
    }

    private ObjectFileEntity requireSourceFile(Long fileId) {
        ObjectFileEntity sourceFile = objectFileMapper.selectById(fileId);
        if (sourceFile == null) {
            throw new BusinessException(400102, "Dataset source file not found");
        }
        return sourceFile;
    }

    private DatasetFileFormat resolveFormat(ObjectFileEntity sourceFile) {
        try {
            return DatasetFileFormat.fromFilename(sourceFile.getOriginalFilename());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400102, "Unsupported dataset file format");
        }
    }

    private void runImport(DatasetImportJobEntity job,
                           ObjectFileEntity sourceFile,
                           DatasetImportRequest request,
                           DatasetImportMode mode,
                           DatasetParser parser,
                           Long actorId) {
        try {
            markRunning(job);
            if (mode == DatasetImportMode.OVERWRITE) {
                // 覆盖导入采用软删除，保留历史题目和变更审计的可追溯性。
                datasetItemMapper.softDeleteActiveByTaskId(job.getTaskId());
            }
            DatasetParseResult result;
            try (InputStream inputStream = objectStorageService.openReadStream(sourceFile.getBucketName(), sourceFile.getObjectKey())) {
                result = parser.parse(inputStream, request.datasetType());
            }

            List<DatasetImportError> errors = new ArrayList<>(result.errors());
            int successCount = 0;
            for (DatasetImportRow row : result.rows()) {
                // 唯一性由数据库约束兜底，这里先做业务检查以生成可读的行级错误报告。
                if (datasetItemMapper.countActiveByTaskIdAndExternalId(job.getTaskId(), row.externalId()) > 0) {
                    errors.add(new DatasetImportError(row.rowNo(), row.externalId(), "DUPLICATE_EXTERNAL_ID",
                            "externalId already exists in this task", row.rawRow()));
                    continue;
                }
                DatasetItemEntity item = toEntity(job.getTaskId(), request.datasetType().name(), row);
                datasetItemMapper.insert(item);
                appendChangeLog(job.getTaskId(), item, actorId);
                successCount++;
            }
            finishJob(job, result.rows().size() + result.errors().size(), successCount, errors, actorId);
        } catch (Throwable failure) {
            // 文件级或系统级异常标记整个任务失败，便于前端轮询展示失败原因。
            job.setStatus(DatasetImportStatus.FAILED.name());
            job.setErrorMessage(failure.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            importJobMapper.updateById(job);
        }
    }

    private void markRunning(DatasetImportJobEntity job) {
        job.setStatus(DatasetImportStatus.RUNNING.name());
        job.setStartedAt(LocalDateTime.now());
        importJobMapper.updateById(job);
    }

    private DatasetItemEntity toEntity(Long taskId, String datasetType, DatasetImportRow row) throws JsonProcessingException {
        DatasetItemEntity item = new DatasetItemEntity();
        item.setTaskId(taskId);
        item.setExternalId(row.externalId());
        item.setDatasetType(datasetType);
        item.setItemJson(objectMapper.writeValueAsString(row.itemJson()));
        item.setMetadataJson(objectMapper.writeValueAsString(row.metadataJson()));
        item.setAssignedCount(0);
        item.setSubmittedCount(0);
        item.setApprovedCount(0);
        item.setDeleted(false);
        return item;
    }

    private void appendChangeLog(Long taskId, DatasetItemEntity item, Long actorId) {
        DatasetItemChangeLogEntity changeLog = new DatasetItemChangeLogEntity();
        changeLog.setTaskId(taskId);
        changeLog.setItemId(item.getId());
        changeLog.setChangeType("IMPORT_APPEND");
        changeLog.setAfterJson(item.getItemJson());
        changeLog.setActorId(actorId);
        changeLogMapper.insert(changeLog);
    }

    private void finishJob(DatasetImportJobEntity job,
                           int totalCount,
                           int successCount,
                           List<DatasetImportError> errors,
                           Long actorId) throws JsonProcessingException {
        int failedCount = errors.size();
        if (!errors.isEmpty()) {
            // 只要存在行级失败就生成报告，成功行仍然正常入库。
            ObjectFileEntity errorFile = uploadErrorReport(job.getId(), errors, actorId);
            job.setErrorReportFileId(errorFile.getId());
        }
        job.setTotalCount(totalCount);
        job.setSuccessCount(successCount);
        job.setFailedCount(failedCount);
        job.setStatus(resolveFinalStatus(successCount, failedCount).name());
        job.setFinishedAt(LocalDateTime.now());
        importJobMapper.updateById(job);
    }

    private DatasetImportStatus resolveFinalStatus(int successCount, int failedCount) {
        if (failedCount == 0) {
            return DatasetImportStatus.SUCCESS;
        }
        if (successCount == 0) {
            return DatasetImportStatus.FAILED;
        }
        return DatasetImportStatus.PARTIAL_SUCCESS;
    }

    private ObjectFileEntity uploadErrorReport(Long jobId, List<DatasetImportError> errors, Long actorId) throws JsonProcessingException {
        String originalFilename = "dataset-import-%d-errors.jsonl".formatted(jobId);
        StringBuilder content = new StringBuilder();
        for (DatasetImportError error : errors) {
            // 错误报告使用 JSONL，便于大文件逐行下载、预览和后续离线分析。
            content.append(objectMapper.writeValueAsString(Map.of(
                    "rowNo", error.rowNo(),
                    "externalId", error.externalId() == null ? "" : error.externalId(),
                    "errorCode", error.errorCode(),
                    "errorMessage", error.errorMessage() == null ? "" : error.errorMessage(),
                    "rawRow", error.rawRow() == null ? objectMapper.nullNode() : error.rawRow()
            ))).append('\n');
        }
        byte[] bytes = content.toString().getBytes(StandardCharsets.UTF_8);
        String objectKey = buildErrorReportObjectKey(originalFilename);
        objectStorageService.upload(storageProperties.bucket(), objectKey, "application/x-ndjson",
                new ByteArrayInputStream(bytes), bytes.length);

        ObjectFileEntity entity = new ObjectFileEntity();
        entity.setOwnerId(actorId);
        entity.setBucketName(storageProperties.bucket());
        entity.setObjectKey(objectKey);
        entity.setOriginalFilename(originalFilename);
        entity.setContentType("application/x-ndjson");
        entity.setFileSize((long) bytes.length);
        entity.setStorageProvider("COS");
        objectFileMapper.insert(entity);
        return entity;
    }

    private String buildErrorReportObjectKey(String originalFilename) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return "uploads/dataset/errors/%04d/%02d/%02d/%s-%s".formatted(
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID(),
                originalFilename.replaceAll("[^A-Za-z0-9._-]", "_")
        );
    }

    private DatasetImportJobResponse toResponse(DatasetImportJobEntity job) {
        String errorReportUrl = null;
        if (job.getErrorReportFileId() != null) {
            ObjectFileEntity errorFile = objectFileMapper.selectById(job.getErrorReportFileId());
            if (errorFile != null) {
                URL url = objectStorageService.generatePresignedDownloadUrl(errorFile.getBucketName(), errorFile.getObjectKey(),
                        errorFile.getOriginalFilename(), Instant.now().plus(storageProperties.signedUrlTtl()));
                errorReportUrl = url.toString();
            }
        }
        return new DatasetImportJobResponse(
                job.getId(),
                job.getTaskId(),
                job.getStatus(),
                job.getImportMode(),
                job.getTotalCount(),
                job.getSuccessCount(),
                job.getFailedCount(),
                job.getErrorReportFileId(),
                errorReportUrl,
                job.getErrorMessage(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getCreatedAt()
        );
    }
}
