package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "审核员整任务领取结果")
public record ReviewTaskClaimResponse(
        @Schema(description = "任务 ID", example = "10")
        Long taskId,
        @Schema(description = "审核级别", example = "1")
        Integer reviewLevel,
        @Schema(description = "本次领取归属到该审核员名下的待审提交数", example = "25")
        int claimedSubmissionCount) {
}
