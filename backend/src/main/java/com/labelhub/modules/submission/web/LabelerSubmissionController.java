package com.labelhub.modules.submission.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.dto.LabelerSubmissionDetailResponse;
import com.labelhub.modules.submission.dto.LabelerSubmissionListItem;
import com.labelhub.modules.submission.service.LabelerSubmissionQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labeler/submissions")
public class LabelerSubmissionController {

    private final LabelerSubmissionQueryService queryService;

    public LabelerSubmissionController(LabelerSubmissionQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ApiResponse<List<LabelerSubmissionListItem>> list(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) SubmissionStatus submissionStatus,
            @RequestParam(required = false) AssignmentStatus assignmentStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(queryService.listSubmissions(
                CurrentUserContext.getUserId(), taskId, submissionStatus, assignmentStatus, page, size));
    }

    @GetMapping("/{submissionId}")
    public ApiResponse<LabelerSubmissionDetailResponse> getDetail(@PathVariable Long submissionId) {
        CurrentUserContext.requireRole(RoleCode.LABELER);
        return ApiResponse.ok(queryService.getDetail(submissionId, CurrentUserContext.getUserId()));
    }
}
