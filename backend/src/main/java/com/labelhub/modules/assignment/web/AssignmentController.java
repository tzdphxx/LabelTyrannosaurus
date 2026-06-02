package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.AssignmentClaimResponse;
import com.labelhub.modules.assignment.dto.BatchClaimRequest;
import com.labelhub.modules.assignment.dto.BatchClaimResponse;
import com.labelhub.modules.assignment.service.AssignmentClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/assignments")
@Tag(name = "标注领取", description = "标注任务领取、批量领取和指派")
public class AssignmentController {

    private final AssignmentClaimService assignmentClaimService;

    public AssignmentController(AssignmentClaimService assignmentClaimService) {
        this.assignmentClaimService = assignmentClaimService;
    }

    @PostMapping("/claim")
    @Operation(summary = "领取任务", description = "当前标注员领取一个可标注的数据项。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<AssignmentClaimResponse> claim(@PathVariable Long taskId) {
        return ApiResponse.ok(assignmentClaimService.claim(taskId, CurrentUserContext.getUserId()));
    }

    @PostMapping("/batch-claim")
    @Operation(summary = "批量领取", description = "一次领取多条可标注的数据项。FCFS 策略领到无库存或配额满。QUOTA_CLAIM 策略领到配额满或库存空。ASSIGNED 策略不可使用，返回 403。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<BatchClaimResponse> batchClaim(
            @Parameter(description = "任务 ID") @PathVariable Long taskId,
            @Valid @RequestBody BatchClaimRequest request) {
        Long labelerId = CurrentUserContext.getUserId();
        List<AssignmentClaimResponse> claimed = new ArrayList<>();
        int failed = 0;
        for (int i = 0; i < request.count(); i++) {
            try {
                claimed.add(assignmentClaimService.claim(taskId, labelerId));
            } catch (Exception e) {
                failed++;
                break; // stop on first failure (no stock, quota full, etc.)
            }
        }
        return ApiResponse.ok(new BatchClaimResponse(claimed, claimed.size(), failed));
    }

    @PostMapping("/assign")
    @Operation(summary = "指派题目", description = "Owner 将指定题目指派给指定标注员。仅 ASSIGNED 策略的任务可用。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<AssignmentClaimResponse>> assign(
            @Parameter(description = "任务 ID") @PathVariable Long taskId,
            @Valid @RequestBody com.labelhub.modules.assignment.dto.AssignRequest request) {
        CurrentUserContext.requireRole(RoleCode.OWNER);
        return ApiResponse.ok(assignmentClaimService.assign(taskId, request.labelerId(), request.datasetItemIds()));
    }
}
