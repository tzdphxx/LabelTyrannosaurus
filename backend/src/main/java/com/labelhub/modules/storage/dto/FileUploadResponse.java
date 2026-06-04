package com.labelhub.modules.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文件上传响应")
public record FileUploadResponse(
        @Schema(description = "文件 ID", example = "99")
        Long fileId,
        @Schema(description = "原始文件名", example = "dataset.jsonl")
        String originalFilename,
        @Schema(description = "文件类型", example = "application/x-ndjson")
        String contentType,
        @Schema(description = "文件大小（字节）", example = "1024")
        Long fileSize,
        @Schema(description = "对象存储 Key", example = "uploads/dataset/file.jsonl")
        String objectKey,
        @Schema(description = "SHA-256 校验和")
        String checksum,
        @Schema(description = "下载地址（预签名 URL）")
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
