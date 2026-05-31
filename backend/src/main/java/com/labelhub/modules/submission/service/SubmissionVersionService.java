package com.labelhub.modules.submission.service;

import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.dto.VersionHistoryItem;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SubmissionVersionService {

    private final SubmissionMapper submissionMapper;
    private final AiReviewResultMapper aiReviewResultMapper;

    public SubmissionVersionService(SubmissionMapper submissionMapper,
                                    AiReviewResultMapper aiReviewResultMapper) {
        this.submissionMapper = submissionMapper;
        this.aiReviewResultMapper = aiReviewResultMapper;
    }

    public List<VersionHistoryItem> getVersionHistory(Long assignmentId) {
        List<Submission> versions = submissionMapper.selectByAssignmentId(assignmentId);
        return versions.stream().map(this::toHistoryItem).toList();
    }

    private VersionHistoryItem toHistoryItem(Submission s) {
        AiReviewResult aiResult = aiReviewResultMapper.selectBySubmissionId(s.getId());
        return new VersionHistoryItem(
                s.getId(),
                s.getVersionNo(),
                s.getStatus(),
                s.getAnswerHash(),
                s.getIsGolden(),
                s.getSubmittedAt(),
                aiResult != null ? aiResult.getDecision() : null,
                aiResult != null ? aiResult.getFlowAction() : null,
                null
        );
    }
}
