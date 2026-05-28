package com.labelhub.modules.assignment.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.assignment.dto.AssignmentDraftResponse;
import com.labelhub.modules.assignment.dto.AssignmentDraftSaveRequest;
import com.labelhub.modules.assignment.service.AssignmentDraftService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assignments/{assignmentId}/draft")
public class AssignmentDraftController {

    private final AssignmentDraftService assignmentDraftService;
    private final CurrentUserContext currentUserContext;

    public AssignmentDraftController(AssignmentDraftService assignmentDraftService,
                                     CurrentUserContext currentUserContext) {
        this.assignmentDraftService = assignmentDraftService;
        this.currentUserContext = currentUserContext;
    }

    @PutMapping
    public ApiResponse<AssignmentDraftResponse> saveDraft(@PathVariable Long assignmentId,
                                                          @Valid @RequestBody AssignmentDraftSaveRequest request) {
        return ApiResponse.ok(assignmentDraftService.saveDraft(
                assignmentId,
                currentUserContext.currentUserId(),
                request
        ));
    }

    @GetMapping
    public ApiResponse<AssignmentDraftResponse> getDraft(@PathVariable Long assignmentId) {
        return ApiResponse.ok(assignmentDraftService.getDraft(
                assignmentId,
                currentUserContext.currentUserId()
        ));
    }
}
