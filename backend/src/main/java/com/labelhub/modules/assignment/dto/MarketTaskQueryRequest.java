package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.task.domain.TaskStatus;

@Schema(description = "标注市场任务查询请求")
public record MarketTaskQueryRequest(
        @Schema(description = "关键词搜索") String keyword,
        @Schema(description = "标签筛选") String tag,
        @Schema(description = "任务状态筛选") TaskStatus status) {
}
