package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.AssignmentDetailResponse;
import com.labelhub.modules.assignment.service.AssignmentDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assignments")
@Tag(name = "标注任务领取", description = "标注员查看已领取的 assignment 详情")
public class AssignmentDetailController {

    private final AssignmentDetailService assignmentDetailService;

    public AssignmentDetailController(AssignmentDetailService assignmentDetailService) {
        this.assignmentDetailService = assignmentDetailService;
    }

    @GetMapping("/{assignmentId}")
    @Operation(summary = "领取详情", description = "查询标注员已领取的 assignment 详情，包含题目数据、模板信息、当前草稿和提交状态。")
    public ApiResponse<AssignmentDetailResponse> getDetail(
            @Parameter(description = "Assignment ID") @PathVariable Long assignmentId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(assignmentDetailService.getDetail(
                assignmentId, CurrentUserContext.getUserId()));
    }
}
