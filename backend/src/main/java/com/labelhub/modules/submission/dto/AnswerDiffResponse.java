package com.labelhub.modules.submission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "答案差异响应")
public record AnswerDiffResponse(
        @Schema(description = "基准提交ID") Long baseSubmissionId,
        @Schema(description = "基准版本号") Integer baseVersionNo,
        @Schema(description = "目标提交ID") Long targetSubmissionId,
        @Schema(description = "目标版本号") Integer targetVersionNo,
        @Schema(description = "字段差异列表") List<FieldDiff> diffs
) {}
