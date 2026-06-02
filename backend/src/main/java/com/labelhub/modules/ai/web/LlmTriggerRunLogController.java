package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.ai.dto.LlmTriggerRunPageResponse;
import com.labelhub.modules.ai.dto.LlmTriggerRunQuery;
import com.labelhub.modules.ai.service.LlmTriggerRunQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/llm-trigger-runs")
@PreAuthorize("hasAnyRole('ADMIN','OWNER','REVIEWER')")
@Tag(name = "LLM 调用日志", description = "任务维度的 LLM 触发器运行日志分页查询")
public class LlmTriggerRunLogController {

    private final LlmTriggerRunQueryService queryService;

    public LlmTriggerRunLogController(LlmTriggerRunQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "LLM 调用日志列表",
            description = "分页查询指定任务下的所有 LLM 触发器运行记录，支持按状态、组件、时间范围筛选。")
    public ApiResponse<LlmTriggerRunPageResponse> list(
            @Parameter(description = "任务 ID") @PathVariable Long taskId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String componentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        LlmTriggerRunQuery query = new LlmTriggerRunQuery(
                taskId, page, pageSize, status, componentId, startTime, endTime);
        return ApiResponse.ok(queryService.listByTask(
                CurrentUserContext.requireCurrentUser(), query));
    }
}
