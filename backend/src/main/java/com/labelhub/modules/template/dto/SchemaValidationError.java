package com.labelhub.modules.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Schema 或答案校验失败的单条明细。
 *
 * <p>path 使用 JSON Pointer 风格，便于前端或 BE-A 精确定位出错字段。</p>
 */
@Schema(description = "Schema或答案校验失败的单条明细")
public record SchemaValidationError(
        @Schema(description = "错误字段路径，JSON Pointer风格") String path,
        @Schema(description = "错误码") int errorCode,
        @Schema(description = "错误信息") String errorMessage) {

    public static SchemaValidationError of(String path, String errorMessage) {
        return new SchemaValidationError(path, 409301, errorMessage);
    }
}
