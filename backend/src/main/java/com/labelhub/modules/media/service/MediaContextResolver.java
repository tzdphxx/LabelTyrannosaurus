package com.labelhub.modules.media.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.infrastructure.storage.ObjectStorageService;
import com.labelhub.modules.media.domain.DatasetItemMediaContextEntity;
import com.labelhub.modules.media.mapper.DatasetItemMediaContextMapper;
import com.labelhub.modules.storage.domain.ObjectFileEntity;
import com.labelhub.modules.storage.repository.ObjectFileMapper;
import com.labelhub.modules.storage.service.FileStorageProperties;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MediaContextResolver {

    private final DatasetItemMediaContextMapper contextMapper;
    private final ObjectFileMapper objectFileMapper;
    private final ObjectStorageService objectStorageService;
    private final FileStorageProperties storageProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MediaContextResolver(DatasetItemMediaContextMapper contextMapper,
                                ObjectFileMapper objectFileMapper,
                                ObjectStorageService objectStorageService,
                                FileStorageProperties storageProperties) {
        this.contextMapper = contextMapper;
        this.objectFileMapper = objectFileMapper;
        this.objectStorageService = objectStorageService;
        this.storageProperties = storageProperties;
    }

    public String resolveItemJson(Long datasetItemId, String fallbackItemJson) {
        if (datasetItemId == null) {
            return fallbackItemJson;
        }
        DatasetItemMediaContextEntity context = contextMapper.selectLatestByDatasetItemId(datasetItemId);
        if (context == null || context.getContextJson() == null || context.getContextJson().isBlank()) {
            return fallbackItemJson;
        }
        Map<String, Object> resolved = parseMap(context.getContextJson());
        resolved.put("media_processing_status", context.getProcessingStatus());
        resolved.put("media_context_limitations", parseList(context.getLimitationsJson()));
        resolveFileUrl(resolved, "media_file_id", "media_url");
        resolveFileUrlList(resolved, "key_frame_file_ids", "key_frame_urls");
        return toJson(resolved);
    }

    private void resolveFileUrl(Map<String, Object> context, String fileIdKey, String urlKey) {
        Long fileId = asLong(context.get(fileIdKey));
        if (fileId == null || context.get(urlKey) != null) {
            return;
        }
        String signedUrl = signedUrl(fileId);
        if (signedUrl != null) {
            context.put(urlKey, signedUrl);
        }
    }

    private void resolveFileUrlList(Map<String, Object> context, String fileIdsKey, String urlsKey) {
        Object value = context.get(fileIdsKey);
        if (!(value instanceof Iterable<?> iterable) || context.get(urlsKey) != null) {
            return;
        }
        List<String> urls = new ArrayList<>();
        for (Object item : iterable) {
            Long fileId = asLong(item);
            if (fileId != null) {
                String signedUrl = signedUrl(fileId);
                if (signedUrl != null) {
                    urls.add(signedUrl);
                }
            }
        }
        if (!urls.isEmpty()) {
            context.put(urlsKey, urls);
        }
    }

    private String signedUrl(Long fileId) {
        ObjectFileEntity file = objectFileMapper.selectById(fileId);
        if (file == null) {
            return null;
        }
        URL url = objectStorageService.generatePresignedDownloadUrl(
                file.getBucketName(),
                file.getObjectKey(),
                file.getOriginalFilename(),
                Instant.now().plus(storageProperties.signedUrlTtl())
        );
        return url.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            return value instanceof Map<?, ?> map ? new LinkedHashMap<>((Map<String, Object>) map) : new LinkedHashMap<>();
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }

    private List<String> parseList(String json) {
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
