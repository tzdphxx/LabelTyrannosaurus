package com.labelhub.modules.template.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 答案校验请求。
 *
 * @param schemaVersionId 模板版本 ID
 * @param answerJson 标注员提交的答案 JSON object
 */
public record ValidateAnswerRequest(@NotNull Long schemaVersionId,
                                    @NotNull Map<String, Object> answerJson) {
}
