package com.labelhub.modules.submission.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.submission.dto.AnswerDiffResponse;
import com.labelhub.modules.submission.dto.VersionHistoryItem;
import com.labelhub.modules.submission.service.AnswerDiffService;
import com.labelhub.modules.submission.service.SubmissionVersionService;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionTraceController {

    private final AnswerDiffService answerDiffService;
    private final SubmissionVersionService versionService;
    private final SubmissionMapper submissionMapper;

    public SubmissionTraceController(AnswerDiffService answerDiffService,
                                     SubmissionVersionService versionService,
                                     SubmissionMapper submissionMapper) {
        this.answerDiffService = answerDiffService;
        this.versionService = versionService;
        this.submissionMapper = submissionMapper;
    }

    @GetMapping("/{submissionId}/diff")
    public ApiResponse<AnswerDiffResponse> diff(
            @PathVariable Long submissionId,
            @RequestParam Integer baseVersionNo) {
        CurrentUserContext.requireAnyRole(Set.of(RoleCode.OWNER, RoleCode.REVIEWER));
        return ApiResponse.ok(answerDiffService.diff(submissionId, baseVersionNo));
    }

    @GetMapping("/{submissionId}/versions")
    public ApiResponse<List<VersionHistoryItem>> versions(@PathVariable Long submissionId) {
        CurrentUserContext.requireAnyRole(Set.of(RoleCode.OWNER, RoleCode.REVIEWER, RoleCode.LABELER));
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return ApiResponse.ok(List.of());
        }
        return ApiResponse.ok(versionService.getVersionHistory(submission.getAssignmentId()));
    }
}
