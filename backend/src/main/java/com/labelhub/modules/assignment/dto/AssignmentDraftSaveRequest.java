package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "标注草稿保存请求")
public record AssignmentDraftSaveRequest(
        @NotBlank @Schema(description = "答案JSON") String answerJson,
        @NotNull @Schema(description = "客户端版本号") Integer clientVersion) {
}
