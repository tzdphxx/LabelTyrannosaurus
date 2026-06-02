package com.labelhub.modules.template.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.template.dto.CreateTemplateRequest;
import com.labelhub.modules.template.dto.ForkTemplateRequest;
import com.labelhub.modules.template.dto.TemplateResponse;
import com.labelhub.modules.template.dto.TemplateVersionResponse;
import com.labelhub.modules.template.service.TemplateService;
import com.labelhub.modules.template.service.TemplateVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模板版本管理接口。
 *
 * <p>接口只处理模板和版本资源；任务发布、暂停和发布版本冻结仍由 BE-A 任务模块负责。</p>
 */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN','OWNER')")
@Tag(name = "模板", description = "任务模板和模板版本管理")
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateVersionService templateVersionService;

    public TemplateController(TemplateService templateService, TemplateVersionService templateVersionService) {
        this.templateService = templateService;
        this.templateVersionService = templateVersionService;
    }

    /**
     * 创建任务模板并生成首个版本。
     */
    @PostMapping("/tasks/{taskId}/templates")
    @Operation(summary = "创建模板", description = "为任务创建模板并生成首个版本。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<TemplateResponse> createTemplate(@PathVariable Long taskId,
                                                        @Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.ok(templateService.createTemplate(taskId, request));
    }

    /**
     * 查询任务下模板列表。
     */
    @GetMapping("/tasks/{taskId}/templates")
    @Operation(summary = "模板列表", description = "查询任务下的模板列表。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<List<TemplateResponse>> listTemplates(@PathVariable Long taskId) {
        return ApiResponse.ok(templateService.listTemplates(taskId));
    }

    /**
     * 查询模板版本详情。
     */
    @GetMapping("/template-versions/{versionId}")
    @Operation(summary = "模板版本详情", description = "查询指定模板版本详情。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<TemplateVersionResponse> getVersion(@PathVariable Long versionId) {
        return ApiResponse.ok(templateVersionService.getVersion(versionId));
    }

    /**
     * 基于已有版本 fork 新版本。
     */
    @PostMapping("/templates/{templateId}/fork")
    @Operation(summary = "Fork 模板", description = "基于已有模板创建新版本。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<TemplateResponse> forkTemplate(@PathVariable Long templateId,
                                                      @RequestBody ForkTemplateRequest request) {
        return ApiResponse.ok(templateService.forkTemplate(templateId, request));
    }
}
