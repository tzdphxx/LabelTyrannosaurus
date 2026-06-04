package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.LabelerTaskDetailResponse;
import com.labelhub.modules.assignment.dto.LabelerTaskTemplateResponse;
import com.labelhub.modules.assignment.service.LabelerTaskWorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labeler/tasks")
@Tag(name = "标注员任务工作台", description = "标注员读取任务详情和答题模板")
public class LabelerTaskWorkspaceController {

    private final LabelerTaskWorkspaceService workspaceService;

    public LabelerTaskWorkspaceController(LabelerTaskWorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/{taskId}/detail")
    @Operation(summary = "标注员任务详情", description = "读取任务详情，并分页返回当前标注员可领取的题目详情。")
    public ApiResponse<LabelerTaskDetailResponse> getTaskDetail(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") int itemPage,
            @RequestParam(defaultValue = "20") int itemSize) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(workspaceService.getTaskDetail(
                CurrentUserContext.getUserId(), taskId, itemPage, itemSize));
    }

    @GetMapping("/{taskId}/answer-template")
    @Operation(summary = "标注员答题模板", description = "读取任务当前发布模板的 schemaJson。")
    public ApiResponse<LabelerTaskTemplateResponse> getAnswerTemplate(@PathVariable Long taskId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(workspaceService.getAnswerTemplate(CurrentUserContext.getUserId(), taskId));
    }
}
