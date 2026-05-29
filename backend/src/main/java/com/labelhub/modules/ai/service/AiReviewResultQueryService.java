package com.labelhub.modules.ai.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.mapper.TaskMapper;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AiReviewResultQueryService {

    private static final int AI_REVIEW_RESULT_NOT_FOUND = 404703;
    private static final int FORBIDDEN = 403703;

    private final AiReviewResultMapper aiReviewResultMapper;
    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final AiAutoReviewService aiAutoReviewService;

    public AiReviewResultQueryService(AiReviewResultMapper aiReviewResultMapper,
                                      SubmissionMapper submissionMapper,
                                      TaskMapper taskMapper,
                                      AiAutoReviewService aiAutoReviewService) {
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
        this.aiAutoReviewService = aiAutoReviewService;
    }

    public AiReviewResultResponse getForSubmission(CurrentUser currentUser, Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        AiReviewResult result = aiReviewResultMapper.selectBySubmissionId(submissionId);
        if (submission == null || result == null) {
            throw new BusinessException(AI_REVIEW_RESULT_NOT_FOUND, "AI review result not found");
        }
        requireAccess(currentUser, submission);
        return aiAutoReviewService.toResponse(result);
    }

    private void requireAccess(CurrentUser currentUser, Submission submission) {
        Set<RoleCode> roles = currentUser.roles();
        if (roles.contains(RoleCode.ADMIN) || roles.contains(RoleCode.REVIEWER)) {
            return;
        }
        if (roles.contains(RoleCode.OWNER)) {
            Task task = taskMapper.selectById(submission.getTaskId());
            if (task != null && currentUser.userId().equals(task.getOwnerId())) {
                return;
            }
        }
        throw new BusinessException(FORBIDDEN, "No permission to read AI review result");
    }
}
