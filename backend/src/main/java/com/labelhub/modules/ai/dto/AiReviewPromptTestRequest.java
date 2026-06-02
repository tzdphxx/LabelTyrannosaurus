package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

@Schema(description = "AI审核提示词测试请求")
public record AiReviewPromptTestRequest(
        @Schema(description = "数据集项快照数据")
        @NotEmpty Map<String, Object> itemSnapshot,
        @Schema(description = "标注答案JSON")
        @NotEmpty Map<String, Object> answerJson
) {
}
