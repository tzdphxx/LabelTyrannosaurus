package com.labelhub.modules.submission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "提交请求")
public record SubmissionSubmitRequest(
        @NotBlank @Schema(description = "答案JSON") String answerJson,
        @NotNull @Schema(description = "客户端版本号") Integer clientVersion) {
}
