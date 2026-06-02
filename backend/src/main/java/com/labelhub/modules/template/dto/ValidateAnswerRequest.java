package com.labelhub.modules.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 答案校验请求。
 *
 * @param schemaVersionId 模板版本 ID
 * @param answerJson 标注员提交的答案 JSON object
 */
@Schema(description = "答案校验请求")
public record ValidateAnswerRequest(
        @NotNull @Schema(description = "模板版本ID") Long schemaVersionId,
        @NotNull @Schema(description = "标注员提交的答案JSON对象") Map<String, Object> answerJson) {
}
