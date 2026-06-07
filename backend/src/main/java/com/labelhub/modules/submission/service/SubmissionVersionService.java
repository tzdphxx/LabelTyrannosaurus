package com.labelhub.modules.submission.service;

import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.dto.VersionHistoryItem;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SubmissionVersionService {

    private final SubmissionMapper submissionMapper;
    private final AiReviewResultMapper aiReviewResultMapper;
    private final SubmissionUserResolver userResolver;

    public SubmissionVersionService(SubmissionMapper submissionMapper,
                                    AiReviewResultMapper aiReviewResultMapper,
                                    SubmissionUserResolver userResolver) {
        this.submissionMapper = submissionMapper;
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.userResolver = userResolver;
    }

    public List<VersionHistoryItem> getVersionHistory(Long assignmentId) {
        List<Submission> versions = submissionMapper.selectByAssignmentId(assignmentId);
        Map<Long, String> userNames = userResolver.resolveCreatorNames(versions);
        Map<Long, AiReviewResult> aiResults = versions.isEmpty()
                ? Collections.emptyMap()
                : aiReviewResultMapper.selectBySubmissionIds(
                                versions.stream().map(Submission::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(AiReviewResult::getSubmissionId,
                                Function.identity(), (first, second) -> first));
        return versions.stream()
                .map(s -> toHistoryItem(s, userNames, aiResults.get(s.getId())))
                .toList();
    }

    private VersionHistoryItem toHistoryItem(Submission s, Map<Long, String> userNames,
                                             AiReviewResult aiResult) {
        Long creatorId = userResolver.effectiveCreatorId(s);
        return new VersionHistoryItem(
                s.getId(),
                s.getVersionNo(),
                s.getStatus(),
                s.getAnswerHash(),
                s.getIsGolden(),
                s.getSubmittedAt(),
                aiResult != null ? aiResult.getDecision() : null,
                aiResult != null ? aiResult.getFlowAction() : null,
                null,
                creatorId,
                userNames.get(creatorId)
        );
    }
}
