package com.labelhub.modules.preannotation.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.preannotation.dto.PreAnnotationRunRequest;
import com.labelhub.modules.preannotation.dto.PreAnnotationResponse;
import com.labelhub.modules.preannotation.service.PreAnnotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "预标注", description = "标注员触发 AI 题目级预标注，获取整题建议答案")
public class PreAnnotationController {

    private final PreAnnotationService preAnnotationService;

    public PreAnnotationController(PreAnnotationService preAnnotationService) {
        this.preAnnotationService = preAnnotationService;
    }

    @PostMapping("/api/v1/assignments/{assignmentId}/pre-annotations/run")
    @Operation(summary = "执行预标注", description = "触发 AI 为当前 assignment 生成整题建议答案。复用任务的 AI 审核配置中的 Provider 和 Prompt。一个 assignment 同时只能有一个预标注在运行。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<PreAnnotationResponse> run(
            @Parameter(description = "Assignment ID") @PathVariable Long assignmentId,
            @Valid @RequestBody(required = false) PreAnnotationRunRequest request) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(preAnnotationService.run(assignmentId, CurrentUserContext.getUserId(), request));
    }

    @GetMapping("/api/v1/assignments/{assignmentId}/pre-annotations/latest")
    @Operation(summary = "最新预标注结果", description = "获取当前 assignment 最新一次预标注的结果，包含建议答案、字段级建议、置信度和风险标记。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<PreAnnotationResponse> latest(
            @Parameter(description = "Assignment ID") @PathVariable Long assignmentId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(preAnnotationService.latest(assignmentId, CurrentUserContext.getUserId()));
    }

    @GetMapping("/api/v1/pre-annotations/{preAnnotationId}")
    @Operation(summary = "预标注详情", description = "查询指定预标注记录的完整信息。LABELER 只能查看自己 assignment 的预标注，OWNER 和 REVIEWER 可查看任意预标注。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<PreAnnotationResponse> detail(
            @Parameter(description = "预标注记录 ID") @PathVariable Long preAnnotationId) {
        return ApiResponse.ok(preAnnotationService.getDetail(preAnnotationId, CurrentUserContext.requireCurrentUser()));
    }
}
