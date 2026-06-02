package com.labelhub.modules.ai.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.ai.dto.AiReviewResultPageResponse;
import com.labelhub.modules.ai.dto.AiReviewResultQuery;
import com.labelhub.modules.ai.service.AiReviewLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/ai-review-logs")
@PreAuthorize("hasAnyRole('ADMIN','OWNER','REVIEWER')")
@Tag(name = "AI 审核日志", description = "任务维度的 AI 审核结果分页查询")
public class AiReviewLogController {

    private final AiReviewLogQueryService queryService;

    public AiReviewLogController(AiReviewLogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "AI 审核日志列表",
            description = "分页查询指定任务下的所有 AI 审核结果，支持按状态、决策、时间范围筛选。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<AiReviewResultPageResponse> list(
            @Parameter(description = "任务 ID") @PathVariable Long taskId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        AiReviewResultQuery query = new AiReviewResultQuery(
                taskId, page, pageSize, status, decision, startTime, endTime);
        return ApiResponse.ok(queryService.listByTask(
                CurrentUserContext.requireCurrentUser(), query));
    }
}
