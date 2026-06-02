package com.labelhub.modules.submission.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.api.PageResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.dto.LabelerSubmissionDetailResponse;
import com.labelhub.modules.submission.dto.LabelerSubmissionListItem;
import com.labelhub.modules.submission.service.LabelerSubmissionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labeler/submissions")
@Tag(name = "标注员提交记录", description = "标注员查看自己的提交列表和详情")
public class LabelerSubmissionController {

    private final LabelerSubmissionQueryService queryService;

    public LabelerSubmissionController(LabelerSubmissionQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "我的提交列表", description = "标注员查看自己的提交记录，支持按任务、提交状态、领取状态筛选，分页返回。")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", description = "请求参数校验失败"), @ApiResponse(responseCode = "401", description = "未认证"), @ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<PageResponse<LabelerSubmissionListItem>> list(
            @Parameter(description = "按任务 ID 筛选") @RequestParam(required = false) Long taskId,
            @Parameter(description = "按提交状态筛选：AI_REVIEWING / PENDING_FINAL / APPROVED / REJECTED") @RequestParam(required = false) SubmissionStatus submissionStatus,
            @Parameter(description = "按领取状态筛选：CLAIMED / SUBMITTED / RETURNED / APPROVED") @RequestParam(required = false) AssignmentStatus assignmentStatus,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(queryService.listSubmissions(
                CurrentUserContext.getUserId(), taskId, submissionStatus, assignmentStatus, page, size));
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "提交详情", description = "查看单条提交的详细信息，包含答案内容、AI 审核结果、审核状态等。")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400", description = "请求参数校验失败"), @ApiResponse(responseCode = "401", description = "未认证"), @ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<LabelerSubmissionDetailResponse> getDetail(
            @Parameter(description = "提交 ID") @PathVariable Long submissionId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(queryService.getDetail(submissionId, CurrentUserContext.getUserId()));
    }
}
