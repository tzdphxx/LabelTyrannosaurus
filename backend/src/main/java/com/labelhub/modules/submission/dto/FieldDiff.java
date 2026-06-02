package com.labelhub.modules.submission.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "字段差异")
public record FieldDiff(
        @Schema(description = "字段名") String field,
        @Schema(description = "修改前的值") Object before,
        @Schema(description = "修改后的值") Object after,
        @Schema(description = "变更类型") ChangeType changeType
) {
    @Schema(description = "变更类型枚举")
    public enum ChangeType {
        ADDED, MODIFIED, REMOVED
    }
}
