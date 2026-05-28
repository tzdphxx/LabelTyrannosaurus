package com.labelhub.modules.submission.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.submission.dto.SubmissionSubmitRequest;
import com.labelhub.modules.submission.dto.SubmissionSubmitResponse;
import com.labelhub.modules.submission.service.SubmissionSubmitService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assignments/{assignmentId}/submit")
public class AssignmentSubmitController {

    private final SubmissionSubmitService submissionSubmitService;
    private final CurrentUserContext currentUserContext;

    public AssignmentSubmitController(SubmissionSubmitService submissionSubmitService,
                                      CurrentUserContext currentUserContext) {
        this.submissionSubmitService = submissionSubmitService;
        this.currentUserContext = currentUserContext;
    }

    @PostMapping
    public ApiResponse<SubmissionSubmitResponse> submit(@PathVariable Long assignmentId,
                                                        @Valid @RequestBody SubmissionSubmitRequest request) {
        return ApiResponse.ok(submissionSubmitService.submit(
                assignmentId,
                currentUserContext.currentUserId(),
                request
        ));
    }
}
