package com.labelhub.modules.preannotation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "预标注运行请求")
public record PreAnnotationRunRequest(
        @Schema(description = "模板版本ID") Long templateVersionId,
        @Schema(description = "数据集题目ID") Long datasetItemId,
        @Schema(description = "当前答案JSON") String currentAnswerJson,
        @Size(max = 30) @Schema(description = "运行模式") String mode
) {
}
