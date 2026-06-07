package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.LabelerAssignmentListItem;
import com.labelhub.modules.assignment.service.AssignmentCancelService;
import com.labelhub.modules.assignment.service.LabelerAssignmentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labeler/assignments")
@Tag(name = "标注员工作台", description = "标注员查看进行中的 assignment 列表和放弃领取")
public class LabelerAssignmentController {

    private final LabelerAssignmentQueryService queryService;
    private final AssignmentCancelService cancelService;

    public LabelerAssignmentController(LabelerAssignmentQueryService queryService,
                                       AssignmentCancelService cancelService) {
        this.queryService = queryService;
        this.cancelService = cancelService;
    }

    @GetMapping
    @Operation(summary = "我的 Assignment 列表",
            description = "分页查询当前标注员的所有 assignment，支持按任务和状态筛选。")
    public ApiResponse<List<LabelerAssignmentListItem>> list(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(queryService.list(
                CurrentUserContext.getUserId(), taskId, status, page, size));
    }

    @PostMapping("/{assignmentId}/cancel")
    @Operation(summary = "放弃领取",
            description = "标注员放弃已领取的 assignment，释放数据项回市场池。"
                    + "仅 CLAIMED/DRAFTING/RETURNED 状态可放弃。")
    public ApiResponse<Void> cancel(
            @Parameter(description = "领取记录 ID") @PathVariable Long assignmentId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        cancelService.cancel(assignmentId, CurrentUserContext.getUserId());
        return ApiResponse.ok(null);
    }
}
