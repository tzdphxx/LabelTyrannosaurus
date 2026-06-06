package com.labelhub.modules.ai.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.dto.AiReviewResultResponse;
import com.labelhub.modules.ai.mapper.AiReviewConfigMapper;
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
    private final AiReviewConfigMapper aiReviewConfigMapper;
    private final AiAutoReviewService aiAutoReviewService;

    public AiReviewResultQueryService(AiReviewResultMapper aiReviewResultMapper,
                                      SubmissionMapper submissionMapper,
                                      TaskMapper taskMapper,
                                      AiReviewConfigMapper aiReviewConfigMapper,
                                      AiAutoReviewService aiAutoReviewService) {
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
        this.aiReviewConfigMapper = aiReviewConfigMapper;
        this.aiAutoReviewService = aiAutoReviewService;
    }

    public AiReviewResultResponse getForSubmission(CurrentUser currentUser, Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        AiReviewResult result = aiReviewResultMapper.selectBySubmissionId(submissionId);
        if (submission == null || result == null) {
            throw new BusinessException(AI_REVIEW_RESULT_NOT_FOUND, "AI 审核结果不存在");
        }
        Task task = taskMapper.selectById(submission.getTaskId());
        requireAccess(currentUser, task);
        return aiAutoReviewService.toResponse(result, rawPrompt(task), submission.getAnswerJson());
    }

    private void requireAccess(CurrentUser currentUser, Task task) {
        Set<RoleCode> roles = currentUser.roles();
        if (roles.contains(RoleCode.ADMIN) || roles.contains(RoleCode.REVIEWER)) {
            return;
        }
        if (roles.contains(RoleCode.OWNER)) {
            if (task != null && currentUser.userId().equals(task.getOwnerId())) {
                return;
            }
        }
        throw new BusinessException(FORBIDDEN, "无权查看 AI 审核结果");
    }

    private String rawPrompt(Task task) {
        if (task == null || task.getAiReviewConfigId() == null) {
            return null;
        }
        AiReviewConfig config = aiReviewConfigMapper.selectById(task.getAiReviewConfigId());
        return config == null ? null : config.getPromptTemplate();
    }
}
