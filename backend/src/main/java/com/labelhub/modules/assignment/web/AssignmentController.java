package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.assignment.dto.AssignmentClaimResponse;
import com.labelhub.modules.assignment.service.AssignmentClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/assignments")
@Tag(name = "标注领取", description = "标注任务领取")
public class AssignmentController {

    private final AssignmentClaimService assignmentClaimService;

    public AssignmentController(AssignmentClaimService assignmentClaimService) {
        this.assignmentClaimService = assignmentClaimService;
    }

    @PostMapping("/claim")
    @Operation(summary = "领取任务", description = "当前标注员领取一个可标注的数据项。")
    public ApiResponse<AssignmentClaimResponse> claim(@PathVariable Long taskId) {
        return ApiResponse.ok(assignmentClaimService.claim(taskId, CurrentUserContext.getUserId()));
    }
}
