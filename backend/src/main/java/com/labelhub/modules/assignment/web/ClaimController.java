package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.AssignmentClaimResponse;
import com.labelhub.modules.assignment.dto.AssignmentDetailResponse;
import com.labelhub.modules.assignment.dto.AssignmentDraftResponse;
import com.labelhub.modules.assignment.dto.AssignmentDraftSaveRequest;
import com.labelhub.modules.assignment.dto.ClaimedTaskResponse;
import com.labelhub.modules.assignment.service.AssignmentClaimService;
import com.labelhub.modules.assignment.service.AssignmentDetailService;
import com.labelhub.modules.assignment.service.AssignmentDraftService;
import com.labelhub.modules.assignment.service.LabelerAssignmentQueryService;
import com.labelhub.modules.submission.dto.SubmissionSubmitRequest;
import com.labelhub.modules.submission.dto.SubmissionSubmitResponse;
import com.labelhub.modules.submission.service.SubmissionSubmitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "领取", description = "标注员领取题目、查看已领取、保存草稿和提交答案")
public class ClaimController {

    private final AssignmentClaimService claimService;
    private final AssignmentDetailService detailService;
    private final AssignmentDraftService draftService;
    private final SubmissionSubmitService submitService;
    private final LabelerAssignmentQueryService queryService;

    public ClaimController(AssignmentClaimService claimService,
                           AssignmentDetailService detailService,
                           AssignmentDraftService draftService,
                           SubmissionSubmitService submitService,
                           LabelerAssignmentQueryService queryService) {
        this.claimService = claimService;
        this.detailService = detailService;
        this.draftService = draftService;
        this.submitService = submitService;
        this.queryService = queryService;
    }

    // ========== 领取题目（需要 taskId 上下文） ==========

    @PostMapping("/api/v1/tasks/{taskId}/items/claim")
    @Operation(summary = "领取题目",
            description = "当前标注员在指定任务下领取一个可标注的题目。三种领取策略（FCFS / QUOTA_GRAB / ASSIGNED）均通过此入口。")
    public ApiResponse<AssignmentClaimResponse> claim(
            @Parameter(description = "任务 ID") @PathVariable Long taskId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(claimService.claim(taskId, CurrentUserContext.getUserId()));
    }

    // ========== 我的领取列表（扁平路径） ==========

    @GetMapping("/api/v1/claims")
    @Operation(summary = "我的领取列表",
            description = "标注员查看自己领取过的题目列表，按领取时间倒序。支持按任务ID和状态筛选。")
    public ApiResponse<List<ClaimedTaskResponse>> listClaims(
            @Parameter(description = "按任务 ID 筛选") @RequestParam(required = false) Long taskId,
            @Parameter(description = "按领取状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，默认 20") @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        Long labelerId = CurrentUserContext.getUserId();
        if (taskId != null) {
            return ApiResponse.ok(List.of(queryService.getClaimedTaskDetail(
                    labelerId, taskId, status, page, size)));
        }
        return ApiResponse.ok(queryService.listClaimedTasks(labelerId, page, size));
    }

    @GetMapping("/api/v1/claims/{claimId}")
    @Operation(summary = "领取详情",
            description = "查询指定领取的详情，包含题目数据、模板信息、当前草稿和提交状态。")
    public ApiResponse<AssignmentDetailResponse> getClaim(
            @Parameter(description = "领取 ID") @PathVariable Long claimId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(detailService.getDetail(claimId, CurrentUserContext.getUserId()));
    }

    // ========== 草稿操作 ==========

    @PutMapping("/api/v1/claims/{claimId}/draft")
    @Operation(summary = "保存草稿", description = "保存当前标注题目的答案草稿，支持增量更新。")
    public ApiResponse<AssignmentDraftResponse> saveDraft(
            @Parameter(description = "领取 ID") @PathVariable Long claimId,
            @Valid @RequestBody AssignmentDraftSaveRequest request) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(draftService.saveDraft(claimId, CurrentUserContext.getUserId(), request));
    }

    @GetMapping("/api/v1/claims/{claimId}/draft")
    @Operation(summary = "读取草稿", description = "读取当前标注题目的草稿内容。")
    public ApiResponse<AssignmentDraftResponse> getDraft(
            @Parameter(description = "领取 ID") @PathVariable Long claimId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(draftService.getDraft(claimId, CurrentUserContext.getUserId()));
    }

    // ========== 提交答案 ==========

    @PostMapping("/api/v1/claims/{claimId}/submit")
    @Operation(summary = "提交标注答案",
            description = "提交当前领取题目的最终标注答案，提交后进入 AI 预审流程。")
    public ApiResponse<SubmissionSubmitResponse> submit(
            @Parameter(description = "领取 ID") @PathVariable Long claimId,
            @Valid @RequestBody SubmissionSubmitRequest request) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(submitService.submit(claimId, CurrentUserContext.getUserId(), request));
    }
}
