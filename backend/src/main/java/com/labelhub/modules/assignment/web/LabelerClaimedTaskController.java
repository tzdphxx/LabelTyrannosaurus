package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.LabelerClaimedTaskResponse;
import com.labelhub.modules.assignment.service.LabelerAssignmentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labeler/claimed-tasks")
@Tag(name = "标注员已领取任务", description = "标注员按任务维度查看已领取的数据项和作答进度")
public class LabelerClaimedTaskController {

    private final LabelerAssignmentQueryService queryService;

    public LabelerClaimedTaskController(LabelerAssignmentQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "已领取任务列表", description = "分页查询当前标注员已领取过数据项的任务列表。")
    public ApiResponse<List<LabelerClaimedTaskResponse>> listClaimedTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(queryService.listClaimedTasks(CurrentUserContext.getUserId(), page, size));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "已领取任务详情", description = "查询指定任务下当前标注员已领取的数据项，支持按领取状态筛选。")
    public ApiResponse<LabelerClaimedTaskResponse> getClaimedTaskDetail(
            @PathVariable Long taskId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(queryService.getClaimedTaskDetail(
                CurrentUserContext.getUserId(), taskId, status, page, size));
    }
}
