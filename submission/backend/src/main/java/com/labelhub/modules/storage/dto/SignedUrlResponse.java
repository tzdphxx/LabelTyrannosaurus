package com.labelhub.modules.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "签名下载地址响应")
public record SignedUrlResponse(
        @Schema(description = "文件 ID", example = "99")
        Long fileId,
        @Schema(description = "短期有效下载地址")
        String downloadUrl
) {
}
