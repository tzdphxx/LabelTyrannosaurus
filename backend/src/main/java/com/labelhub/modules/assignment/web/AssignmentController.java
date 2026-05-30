package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.assignment.dto.AssignmentClaimResponse;
import com.labelhub.modules.assignment.service.AssignmentClaimService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/assignments")
public class AssignmentController {

    private final AssignmentClaimService assignmentClaimService;

    public AssignmentController(AssignmentClaimService assignmentClaimService) {
        this.assignmentClaimService = assignmentClaimService;
    }

    @PostMapping("/claim")
    public ApiResponse<AssignmentClaimResponse> claim(@PathVariable Long taskId) {
        return ApiResponse.ok(assignmentClaimService.claim(taskId, CurrentUserContext.getUserId()));
    }
}
