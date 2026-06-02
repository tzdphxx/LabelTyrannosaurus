package com.labelhub.modules.task.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "标注分发策略")
public enum Strategy {
    @Schema(description = "先到先得：标注员主动领取，先到先得")
    FCFS,
    @Schema(description = "指派：由 Owner 指派标注员，不允许主动领取")
    ASSIGNED,
    @Schema(description = "配额抢单：先到先得 + 每人限制领取数量")
    QUOTA_CLAIM
}
