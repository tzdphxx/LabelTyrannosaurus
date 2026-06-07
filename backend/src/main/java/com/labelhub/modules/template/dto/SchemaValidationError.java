package com.labelhub.modules.template.dto;

/**
 * Schema 或答案校验失败的单条明细。
 *
 * <p>path 使用 JSON Pointer 风格，便于前端或 BE-A 精确定位出错字段。</p>
 */
public record SchemaValidationError(String path,
                                    int errorCode,
                                    String errorMessage) {

    public static SchemaValidationError of(String path, String errorMessage) {
        return new SchemaValidationError(path, 409301, errorMessage);
    }
}
