package com.labelhub.modules.storage.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.infrastructure.storage.ObjectStorageService;
import com.labelhub.modules.storage.domain.ObjectFileEntity;
import com.labelhub.modules.storage.dto.FileUploadResponse;
import com.labelhub.modules.storage.dto.SignedUrlResponse;
import com.labelhub.modules.storage.repository.ObjectFileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "json", "jsonl", "csv", "xlsx", "xls", "txt",
            "jpg", "jpeg", "png", "webp", "gif",
            "mp4", "mov", "webm",
            "mp3", "wav", "m4a",
            "md"
    );
    private static final Set<String> SUPPORTED_BUSINESS_TYPES = Set.of("dataset", "template", "export", "misc", "media");

    private final ObjectFileMapper objectFileMapper;
    private final ObjectStorageService objectStorageService;
    private final FileStorageProperties properties;

    public FileService(ObjectFileMapper objectFileMapper,
                       ObjectStorageService objectStorageService,
                       FileStorageProperties properties) {
        this.objectFileMapper = objectFileMapper;
        this.objectStorageService = objectStorageService;
        this.properties = properties;
    }

    @Transactional
    public FileUploadResponse upload(MultipartFile file, String businessType) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        validate(file, businessType);
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";
        String objectKey = buildObjectKey(businessType, originalFilename);
        byte[] bytes;
        try (InputStream inputStream = file.getInputStream()) {
            bytes = inputStream.readAllBytes();
            objectStorageService.upload(properties.bucket(), objectKey, contentType,
                    new java.io.ByteArrayInputStream(bytes), file.getSize());
        } catch (IOException ex) {
            throw new BusinessException(500001, "文件上传失败");
        }
        String checksum = sha256(bytes);

        ObjectFileEntity entity = new ObjectFileEntity();
        entity.setOwnerId(currentUser.userId());
        entity.setBucketName(properties.bucket());
        entity.setObjectKey(objectKey);
        entity.setOriginalFilename(originalFilename);
        entity.setContentType(contentType);
        entity.setFileSize(file.getSize());
        entity.setChecksum(checksum);
        entity.setStorageProvider("COS");
        objectFileMapper.insert(entity);

        URL downloadUrl = objectStorageService.generatePresignedDownloadUrl(
                properties.bucket(),
                objectKey,
                originalFilename,
                Instant.now().plus(properties.signedUrlTtl())
        );
        return new FileUploadResponse(entity.getId(), originalFilename, contentType, file.getSize(), objectKey,
                checksum, downloadUrl.toString());
    }

    public SignedUrlResponse generateSignedUrl(Long fileId) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        ObjectFileEntity file = objectFileMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(400102, "文件不存在");
        }
        if (!canRead(currentUser, file)) {
            throw new BusinessException(403001, "当前账号没有权限执行该操作");
        }
        URL downloadUrl = objectStorageService.generatePresignedDownloadUrl(
                file.getBucketName(),
                file.getObjectKey(),
                file.getOriginalFilename(),
                Instant.now().plus(properties.signedUrlTtl())
        );
        return new SignedUrlResponse(file.getId(), downloadUrl.toString());
    }

    private void validate(MultipartFile file, String businessType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400102, "文件不能为空");
        }
        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw new BusinessException(400102, "文件大小超出限制");
        }
        if (!SUPPORTED_BUSINESS_TYPES.contains(normalize(businessType))) {
            throw new BusinessException(400102, "Invalid business type");
        }
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = extensionOf(filename);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400102, "不支持的文件类型");
        }
    }

    private boolean canRead(CurrentUser currentUser, ObjectFileEntity file) {
        if (currentUser.roles().contains(RoleCode.ADMIN)) {
            return true;
        }
        return file.getOwnerId() != null && file.getOwnerId().equals(currentUser.userId());
    }

    private String buildObjectKey(String businessType, String originalFilename) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return "uploads/%s/%04d/%02d/%02d/%s-%s".formatted(
                normalize(businessType),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID(),
                sanitizeFilename(originalFilename)
        );
    }

    private String sanitizeFilename(String originalFilename) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename : "file";
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String extensionOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
