package com.labelhub.modules.dataset.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "题目状态")
public enum DatasetItemStatus {
    @Schema(description = "可领取：还有剩余名额")
    AVAILABLE,
    @Schema(description = "名额已满：已被领完")
    FULL
}
