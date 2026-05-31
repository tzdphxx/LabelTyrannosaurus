package com.labelhub.modules.template.controller;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.modules.template.dto.CreateTemplateRequest;
import com.labelhub.modules.template.dto.ForkTemplateRequest;
import com.labelhub.modules.template.dto.TemplateResponse;
import com.labelhub.modules.template.dto.TemplateVersionResponse;
import com.labelhub.modules.template.service.TemplateService;
import com.labelhub.modules.template.service.TemplateVersionService;
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
    public ApiResponse<TemplateResponse> createTemplate(@PathVariable Long taskId,
                                                        @Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.ok(templateService.createTemplate(taskId, request));
    }

    /**
     * 查询任务下模板列表。
     */
    @GetMapping("/tasks/{taskId}/templates")
    public ApiResponse<List<TemplateResponse>> listTemplates(@PathVariable Long taskId) {
        return ApiResponse.ok(templateService.listTemplates(taskId));
    }

    /**
     * 查询模板版本详情。
     */
    @GetMapping("/template-versions/{versionId}")
    public ApiResponse<TemplateVersionResponse> getVersion(@PathVariable Long versionId) {
        return ApiResponse.ok(templateVersionService.getVersion(versionId));
    }

    /**
     * 基于已有版本 fork 新版本。
     */
    @PostMapping("/templates/{templateId}/fork")
    public ApiResponse<TemplateResponse> forkTemplate(@PathVariable Long templateId,
                                                      @RequestBody ForkTemplateRequest request) {
        return ApiResponse.ok(templateService.forkTemplate(templateId, request));
    }
}
