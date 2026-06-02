package com.labelhub.modules.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "File upload response")
public record FileUploadResponse(
        @Schema(description = "File ID", example = "99")
        Long fileId,
        @Schema(description = "Original filename", example = "dataset.jsonl")
        String originalFilename,
        @Schema(description = "Content type", example = "application/x-ndjson")
        String contentType,
        @Schema(description = "File size in bytes", example = "1024")
        Long fileSize,
        @Schema(description = "Object storage key", example = "uploads/dataset/file.jsonl")
        String objectKey,
        @Schema(description = "SHA-256 checksum")
        String checksum,
        @Schema(description = "Download URL or signed URL")
        String downloadUrl
) {
    public FileUploadResponse(Long fileId,
                              String originalFilename,
                              String contentType,
                              Long fileSize,
                              String objectKey,
                              String downloadUrl) {
        this(fileId, originalFilename, contentType, fileSize, objectKey, null, downloadUrl);
    }
}
