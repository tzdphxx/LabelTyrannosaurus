package com.labelhub.modules.media.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.async.AsyncJobCommand;
import com.labelhub.infrastructure.async.AsyncJobService;
import com.labelhub.infrastructure.async.AsyncJobType;
import com.labelhub.modules.dataset.domain.DatasetItemEntity;
import com.labelhub.modules.dataset.repository.DatasetItemRepositoryMapper;
import com.labelhub.modules.media.domain.DatasetItemMediaContextEntity;
import com.labelhub.modules.media.domain.MediaAssetEntity;
import com.labelhub.modules.media.domain.MediaProcessingJobEntity;
import com.labelhub.modules.media.domain.MediaProcessingStatus;
import com.labelhub.modules.media.dto.MediaContextResponse;
import com.labelhub.modules.media.dto.MediaProcessingJobResponse;
import com.labelhub.modules.media.mapper.DatasetItemMediaContextMapper;
import com.labelhub.modules.media.mapper.MediaAssetMapper;
import com.labelhub.modules.media.mapper.MediaDerivativeMapper;
import com.labelhub.modules.media.mapper.MediaProcessingJobMapper;
import com.labelhub.modules.storage.domain.ObjectFileEntity;
import com.labelhub.modules.storage.repository.ObjectFileMapper;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.repository.TaskRepositoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MediaProcessingService {

    private final TaskRepositoryMapper taskMapper;
    private final DatasetItemRepositoryMapper datasetItemMapper;
    private final ObjectFileMapper objectFileMapper;
    private final MediaAssetMapper mediaAssetMapper;
    private final MediaDerivativeMapper mediaDerivativeMapper;
    private final DatasetItemMediaContextMapper contextMapper;
    private final MediaProcessingJobMapper jobMapper;
    private final AsyncJobService asyncJobService;
    private final ObjectMapper objectMapper;

    public MediaProcessingService(TaskRepositoryMapper taskMapper,
                                  DatasetItemRepositoryMapper datasetItemMapper,
                                  ObjectFileMapper objectFileMapper,
                                  MediaAssetMapper mediaAssetMapper,
                                  MediaDerivativeMapper mediaDerivativeMapper,
                                  DatasetItemMediaContextMapper contextMapper,
                                  MediaProcessingJobMapper jobMapper,
                                  AsyncJobService asyncJobService,
                                  ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.datasetItemMapper = datasetItemMapper;
        this.objectFileMapper = objectFileMapper;
        this.mediaAssetMapper = mediaAssetMapper;
        this.mediaDerivativeMapper = mediaDerivativeMapper;
        this.contextMapper = contextMapper;
        this.jobMapper = jobMapper;
        this.asyncJobService = asyncJobService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void refreshContext(Long taskId, Long datasetItemId, String itemJson, Long actorId) {
        Map<String, Object> item = parseMap(itemJson);
        String mediaType = text(item.getOrDefault("media_type", "text")).toLowerCase(Locale.ROOT);
        List<String> limitations = new ArrayList<>();
        Map<String, Object> context = new LinkedHashMap<>(item);
        context.put("media_type", mediaType);

        mediaDerivativeMapper.deleteByDatasetItemId(datasetItemId);
        mediaAssetMapper.deleteByDatasetItemId(datasetItemId);
        Long mediaFileId = asLong(item.get("media_file_id"));
        if (mediaFileId != null) {
            ObjectFileEntity sourceFile = objectFileMapper.selectById(mediaFileId);
            if (sourceFile == null) {
                limitations.add("MEDIA_FILE_MISSING");
            } else {
                insertAsset(taskId, datasetItemId, sourceFile, mediaType, statusFor(mediaType, item, limitations));
            }
        }

        MediaProcessingStatus status = statusFor(mediaType, item, limitations);
        upsertContext(taskId, datasetItemId, mediaType, status, context, limitations);
    }

    @Transactional
    public MediaProcessingJobResponse triggerProcessing(Long datasetItemId) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        DatasetItemEntity item = requireDatasetItem(datasetItemId);
        TaskEntity task = requireReadableTask(item.getTaskId(), currentUser);

        MediaProcessingJobEntity job = new MediaProcessingJobEntity();
        job.setDatasetItemId(item.getId());
        job.setTaskId(task.getId());
        job.setStatus(MediaProcessingStatus.PENDING.name());
        job.setTotalAssets(0);
        job.setProcessedAssets(0);
        job.setCreatedBy(currentUser.userId());
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        jobMapper.insert(job);

        asyncJobService.submit(new AsyncJobCommand(
                AsyncJobType.MEDIA_PROCESSING,
                job.getId(),
                null,
                () -> runProcessing(job.getId())
        ));
        return toJobResponse(job);
    }

    public MediaContextResponse getContext(Long datasetItemId) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        DatasetItemEntity item = requireDatasetItem(datasetItemId);
        requireReadableTask(item.getTaskId(), currentUser);
        DatasetItemMediaContextEntity context = contextMapper.selectLatestByDatasetItemId(datasetItemId);
        if (context == null) {
            return new MediaContextResponse(datasetItemId, item.getTaskId(), "text", "READY",
                    parseMap(item.getItemJson()), List.of(), null);
        }
        return toContextResponse(context);
    }

    public MediaProcessingJobResponse getJob(Long jobId) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        MediaProcessingJobEntity job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(400102, "Media processing job not found");
        }
        requireReadableTask(job.getTaskId(), currentUser);
        return toJobResponse(job);
    }

    @Transactional
    public void runProcessing(Long jobId) {
        MediaProcessingJobEntity job = jobMapper.selectById(jobId);
        if (job == null) {
            return;
        }
        DatasetItemEntity item = requireDatasetItem(job.getDatasetItemId());
        try {
            job.setStatus(MediaProcessingStatus.RUNNING.name());
            job.setStartedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            jobMapper.updateById(job);

            refreshContext(item.getTaskId(), item.getId(), item.getItemJson(), job.getCreatedBy());
            List<MediaAssetEntity> assets = mediaAssetMapper.selectByDatasetItemId(item.getId());
            job.setTotalAssets(assets.size());
            job.setProcessedAssets(assets.size());
            DatasetItemMediaContextEntity context = contextMapper.selectLatestByDatasetItemId(item.getId());
            job.setStatus(context == null ? MediaProcessingStatus.READY.name() : context.getProcessingStatus());
            job.setFinishedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            jobMapper.updateById(job);
        } catch (RuntimeException ex) {
            job.setStatus(MediaProcessingStatus.FAILED.name());
            job.setErrorMessage(ex.getMessage());
            job.setFinishedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            jobMapper.updateById(job);
        }
    }

    private MediaAssetEntity insertAsset(Long taskId,
                                         Long datasetItemId,
                                         ObjectFileEntity sourceFile,
                                         String mediaType,
                                         MediaProcessingStatus status) {
        MediaAssetEntity asset = new MediaAssetEntity();
        asset.setTaskId(taskId);
        asset.setDatasetItemId(datasetItemId);
        asset.setSourceFileId(sourceFile.getId());
        asset.setMediaType(mediaType);
        asset.setContentType(sourceFile.getContentType());
        asset.setFileSize(sourceFile.getFileSize());
        asset.setChecksum(sourceFile.getChecksum());
        asset.setStatus(status.name());
        asset.setMetadataJson(toJson(Map.of(
                "objectKey", sourceFile.getObjectKey(),
                "originalFilename", sourceFile.getOriginalFilename()
        )));
        asset.setLimitationsJson("[]");
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        mediaAssetMapper.insert(asset);
        return asset;
    }

    private void upsertContext(Long taskId,
                               Long datasetItemId,
                               String mediaType,
                               MediaProcessingStatus status,
                               Map<String, Object> contextJson,
                               List<String> limitations) {
        DatasetItemMediaContextEntity context = contextMapper.selectLatestByDatasetItemId(datasetItemId);
        if (context == null) {
            context = new DatasetItemMediaContextEntity();
            context.setTaskId(taskId);
            context.setDatasetItemId(datasetItemId);
            context.setCreatedAt(LocalDateTime.now());
        }
        context.setMediaType(mediaType);
        context.setProcessingStatus(status.name());
        context.setContextJson(toJson(contextJson));
        context.setLimitationsJson(toJson(limitations));
        context.setUpdatedAt(LocalDateTime.now());
        if (context.getId() == null) {
            contextMapper.insert(context);
        } else {
            contextMapper.updateById(context);
        }
    }

    private MediaProcessingStatus statusFor(String mediaType, Map<String, Object> item, List<String> limitations) {
        return switch (mediaType) {
            case "video" -> {
                if (!hasList(item.get("key_frame_urls")) && !hasList(item.get("key_frame_file_ids"))) {
                    addOnce(limitations, "KEY_FRAME_MISSING");
                }
                if (text(item.get("video_transcript")).isBlank() && item.get("transcript_file_id") == null) {
                    addOnce(limitations, "TRANSCRIPT_MISSING");
                }
                yield limitations.isEmpty() ? MediaProcessingStatus.READY : MediaProcessingStatus.PARTIAL;
            }
            case "image", "markdown", "text" -> limitations.isEmpty() ? MediaProcessingStatus.READY : MediaProcessingStatus.PARTIAL;
            default -> {
                addOnce(limitations, "MEDIA_TYPE_UNSUPPORTED");
                yield MediaProcessingStatus.PARTIAL;
            }
        };
    }

    private DatasetItemEntity requireDatasetItem(Long datasetItemId) {
        DatasetItemEntity item = datasetItemMapper.selectById(datasetItemId);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            throw new BusinessException(400102, "Dataset item not found");
        }
        return item;
    }

    private TaskEntity requireReadableTask(Long taskId, CurrentUser currentUser) {
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(400102, "Task not found");
        }
        if (!currentUser.roles().contains(RoleCode.ADMIN) && !currentUser.userId().equals(task.getOwnerId())) {
            throw new BusinessException(403001, "Forbidden");
        }
        return task;
    }

    private MediaProcessingJobResponse toJobResponse(MediaProcessingJobEntity job) {
        return new MediaProcessingJobResponse(
                job.getId(),
                job.getDatasetItemId(),
                job.getTaskId(),
                job.getStatus(),
                job.getTotalAssets(),
                job.getProcessedAssets(),
                job.getErrorMessage(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getCreatedAt()
        );
    }

    private MediaContextResponse toContextResponse(DatasetItemMediaContextEntity context) {
        return new MediaContextResponse(
                context.getDatasetItemId(),
                context.getTaskId(),
                context.getMediaType(),
                context.getProcessingStatus(),
                parseMap(context.getContextJson()),
                parseStringList(context.getLimitationsJson()),
                context.getUpdatedAt()
        );
    }

    private void addOnce(List<String> limitations, String value) {
        if (!limitations.contains(value)) {
            limitations.add(value);
        }
    }

    private boolean hasList(Object value) {
        return value instanceof Iterable<?> iterable && iterable.iterator().hasNext();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            if (!(value instanceof Iterable<?> iterable)) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (Object item : iterable) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
