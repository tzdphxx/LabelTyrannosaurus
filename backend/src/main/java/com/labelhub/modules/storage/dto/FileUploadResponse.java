package com.labelhub.modules.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文件上传响应")
public record FileUploadResponse(
        @Schema(description = "文件 ID", example = "99")
        Long fileId,
        @Schema(description = "原始文件名", example = "dataset.jsonl")
        String originalFilename,
        @Schema(description = "内容类型", example = "application/x-ndjson")
        String contentType,
        @Schema(description = "文件大小，单位字节", example = "1024")
        Long fileSize,
        @Schema(description = "对象存储路径", example = "uploads/dataset/file.jsonl")
        String objectKey,
        @Schema(description = "下载地址或签名地址")
        String downloadUrl
) {
}
