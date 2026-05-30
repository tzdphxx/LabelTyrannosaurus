package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.dto.AssignmentDetailResponse;
import com.labelhub.modules.assignment.service.AssignmentDetailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentDetailController {

    private final AssignmentDetailService assignmentDetailService;

    public AssignmentDetailController(AssignmentDetailService assignmentDetailService) {
        this.assignmentDetailService = assignmentDetailService;
    }

    @GetMapping("/{assignmentId}")
    public ApiResponse<AssignmentDetailResponse> getDetail(@PathVariable Long assignmentId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(assignmentDetailService.getDetail(
                assignmentId, CurrentUserContext.getUserId()));
    }
}
