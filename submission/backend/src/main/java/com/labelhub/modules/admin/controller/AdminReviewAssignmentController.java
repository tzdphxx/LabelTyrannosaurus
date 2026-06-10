package com.labelhub.modules.admin.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.api.PageResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.admin.dto.AssignableReviewTaskResponse;
import com.labelhub.modules.admin.dto.AssignableReviewerResponse;
import com.labelhub.modules.admin.dto.ReviewerProgressResponse;
import com.labelhub.modules.admin.service.AdminReviewAssignmentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/review")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin 审核分配", description = "管理员查看可分配任务、审核员负载和审核进度")
public class AdminReviewAssignmentController {

    private final AdminReviewAssignmentQueryService queryService;

    public AdminReviewAssignmentController(AdminReviewAssignmentQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/tasks/assignable")
    @Operation(summary = "可分配审核任务", description = "按任务和审核级别聚合待终审提交，默认只返回尚未被整任务认领的任务级别。")
    public ApiResponse<PageResponse<AssignableReviewTaskResponse>> listAssignableTasks(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer reviewLevel,
            @RequestParam(defaultValue = "false") boolean includeClaimed,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.ADMIN);
        return ApiResponse.ok(queryService.listAssignableTasks(
                taskId, keyword, reviewLevel, includeClaimed, page, size));
    }

    @GetMapping("/reviewers/assignable")
    @Operation(summary = "可分配人工审核员", description = "返回具备 REVIEWER 角色的人工审核员及其当前负载。")
    public ApiResponse<PageResponse<AssignableReviewerResponse>> listAssignableReviewers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "true") boolean enabledOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.ADMIN);
        return ApiResponse.ok(queryService.listAssignableReviewers(keyword, enabledOnly, page, size));
    }

    @GetMapping("/reviewers/progress")
    @Operation(summary = "人工审核员任务进度", description = "返回审核员待审、今日已审、历史通过率和已认领任务。")
    public ApiResponse<List<ReviewerProgressResponse>> listReviewerProgress(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "true") boolean enabledOnly) {
        CurrentUserContext.requireRole(RoleCode.ADMIN);
        return ApiResponse.ok(queryService.listReviewerProgress(keyword, enabledOnly));
    }
}
