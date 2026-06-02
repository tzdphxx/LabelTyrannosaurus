package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.assignment.dto.AssignmentDraftResponse;
import com.labelhub.modules.assignment.dto.AssignmentDraftSaveRequest;
import com.labelhub.modules.assignment.service.AssignmentDraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assignments/{assignmentId}/draft")
@Tag(name = "标注领取", description = "标注草稿保存和读取")
public class AssignmentDraftController {

    private final AssignmentDraftService assignmentDraftService;

    public AssignmentDraftController(AssignmentDraftService assignmentDraftService) {
        this.assignmentDraftService = assignmentDraftService;
    }

    @PutMapping
    @Operation(summary = "保存草稿", description = "保存当前标注任务的答案草稿。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<AssignmentDraftResponse> saveDraft(@PathVariable Long assignmentId,
                                                          @Valid @RequestBody AssignmentDraftSaveRequest request) {
        return ApiResponse.ok(assignmentDraftService.saveDraft(
                assignmentId,
                CurrentUserContext.getUserId(),
                request
        ));
    }

    @GetMapping
    @Operation(summary = "读取草稿", description = "读取当前标注任务的草稿内容。")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数校验失败"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")})
    public ApiResponse<AssignmentDraftResponse> getDraft(@PathVariable Long assignmentId) {
        return ApiResponse.ok(assignmentDraftService.getDraft(
                assignmentId,
                CurrentUserContext.getUserId()
        ));
    }
}
