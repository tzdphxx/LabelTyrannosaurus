package com.labelhub.modules.submission.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.dto.SubmissionItemHistoryResponse;
import com.labelhub.modules.submission.dto.SubmissionItemHistoryResponse.AiReviewHistory;
import com.labelhub.modules.submission.dto.SubmissionItemHistoryResponse.HistoryItem;
import com.labelhub.modules.submission.dto.SubmissionItemHistoryResponse.ReviewRoundHistory;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SubmissionItemHistoryService {

    private static final int SUBMISSION_NOT_FOUND = 404801;
    private static final int LABELER_FORBIDDEN = 403501;
    private static final int REVIEWER_NOT_ASSIGNED = 403601;
    private static final int FORBIDDEN = 403001;

    private final SubmissionMapper submissionMapper;
    private final AiReviewResultMapper aiReviewResultMapper;
    private final AgentRunMapper agentRunMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final UserMapper userMapper;
    private final SubmissionUserResolver userResolver;

    public SubmissionItemHistoryService(SubmissionMapper submissionMapper,
                                        AiReviewResultMapper aiReviewResultMapper,
                                        AgentRunMapper agentRunMapper,
                                        ReviewRecordMapper reviewRecordMapper,
                                        UserMapper userMapper,
                                        SubmissionUserResolver userResolver) {
        this.submissionMapper = submissionMapper;
        this.aiReviewResultMapper = aiReviewResultMapper;
        this.agentRunMapper = agentRunMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.userMapper = userMapper;
        this.userResolver = userResolver;
    }

    public SubmissionItemHistoryResponse getItemHistory(Long submissionId) {
        Submission requested = submissionMapper.selectById(submissionId);
        if (requested == null) {
            throw new BusinessException(SUBMISSION_NOT_FOUND, "Submission not found");
        }

        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        Long labelerScope = resolveLabelerScope(requested, currentUser);
        List<Submission> submissions = submissionMapper.selectItemHistorySubmissions(
                requested.getTaskId(), requested.getDatasetItemId(), labelerScope);

        List<Long> submissionIds = submissions.stream().map(Submission::getId).toList();
        Map<Long, AiReviewResult> aiResults = aiResultsBySubmissionId(submissionIds);
        Map<Long, AgentRun> agentRuns = agentRunsById(aiResults.values());
        Map<Long, List<ReviewRecord>> reviewRecords = reviewRecordsBySubmissionId(submissionIds);
        Map<Long, String> submitterNames = userResolver.resolveCreatorNames(submissions);
        Map<Long, String> reviewerNames = reviewerNames(reviewRecords.values());

        List<HistoryItem> histories = submissions.stream()
                .map(submission -> toHistoryItem(submission, submitterNames, aiResults,
                        agentRuns, reviewRecords, reviewerNames))
                .toList();

        return new SubmissionItemHistoryResponse(
                requested.getTaskId(), requested.getDatasetItemId(), histories);
    }

    private Long resolveLabelerScope(Submission requested, CurrentUser currentUser) {
        Set<RoleCode> roles = currentUser.roles();
        Long userId = currentUser.userId();
        if (roles.contains(RoleCode.LABELER)) {
            if (!Objects.equals(requested.getLabelerId(), userId)) {
                throw new BusinessException(LABELER_FORBIDDEN,
                        "Current labeler cannot read another labeler's submission history");
            }
            return userId;
        }
        if (roles.contains(RoleCode.REVIEWER)) {
            if (!Objects.equals(requested.getAssignedReviewerId(), userId)) {
                throw new BusinessException(REVIEWER_NOT_ASSIGNED,
                        "Reviewer is not assigned to this submission");
            }
            return null;
        }
        if (roles.contains(RoleCode.OWNER) || roles.contains(RoleCode.ADMIN)) {
            return null;
        }
        throw new BusinessException(FORBIDDEN, "Current account cannot read submission item history");
    }

    private Map<Long, AiReviewResult> aiResultsBySubmissionId(List<Long> submissionIds) {
        if (submissionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return aiReviewResultMapper.selectBySubmissionIds(submissionIds).stream()
                .collect(Collectors.toMap(AiReviewResult::getSubmissionId,
                        Function.identity(), (first, second) -> first));
    }

    private Map<Long, AgentRun> agentRunsById(Collection<AiReviewResult> aiResults) {
        List<Long> runIds = aiResults.stream()
                .map(AiReviewResult::getEffectiveRunId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (runIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return agentRunMapper.selectBatchIds(runIds).stream()
                .collect(Collectors.toMap(AgentRun::getId,
                        Function.identity(), (first, second) -> first));
    }

    private Map<Long, List<ReviewRecord>> reviewRecordsBySubmissionId(List<Long> submissionIds) {
        if (submissionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return reviewRecordMapper.selectBySubmissionIds(submissionIds).stream()
                .collect(Collectors.groupingBy(ReviewRecord::getSubmissionId));
    }

    private Map<Long, String> reviewerNames(Collection<List<ReviewRecord>> groupedRecords) {
        Set<Long> reviewerIds = groupedRecords.stream()
                .flatMap(Collection::stream)
                .map(ReviewRecord::getReviewerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (reviewerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new HashMap<>();
        for (UserEntity user : userMapper.selectBatchIds(reviewerIds)) {
            names.put(user.getId(), displayName(user));
        }
        return names;
    }

    private HistoryItem toHistoryItem(Submission submission,
                                      Map<Long, String> submitterNames,
                                      Map<Long, AiReviewResult> aiResults,
                                      Map<Long, AgentRun> agentRuns,
                                      Map<Long, List<ReviewRecord>> reviewRecords,
                                      Map<Long, String> reviewerNames) {
        Long submitterId = userResolver.effectiveCreatorId(submission);
        AiReviewResult aiResult = aiResults.get(submission.getId());
        return new HistoryItem(
                submission.getId(),
                submission.getAssignmentId(),
                submission.getVersionNo(),
                submission.getStatus(),
                submitterId,
                submitterNames.get(submitterId),
                submission.getSubmittedAt(),
                toAiReview(aiResult, agentRuns),
                toReviewRounds(reviewRecords.getOrDefault(submission.getId(), List.of()), reviewerNames)
        );
    }

    private AiReviewHistory toAiReview(AiReviewResult result, Map<Long, AgentRun> agentRuns) {
        if (result == null) {
            return null;
        }
        return new AiReviewHistory(
                result.getId(),
                result.getEffectiveRunId(),
                result.getStatus(),
                result.getDecision(),
                reviewedAt(result, agentRuns.get(result.getEffectiveRunId()))
        );
    }

    private LocalDateTime reviewedAt(AiReviewResult result, AgentRun run) {
        if (run != null && run.getFinishedAt() != null) {
            return run.getFinishedAt();
        }
        if (result.getUpdatedAt() != null) {
            return result.getUpdatedAt();
        }
        return result.getCreatedAt();
    }

    private List<ReviewRoundHistory> toReviewRounds(List<ReviewRecord> records,
                                                    Map<Long, String> reviewerNames) {
        return records.stream()
                .sorted(Comparator
                        .comparing(ReviewRecord::getReviewLevel,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ReviewRecord::getCreatedAt,
                                Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(ReviewRecord::getId,
                                Comparator.nullsLast(Long::compareTo)))
                .map(record -> new ReviewRoundHistory(
                        record.getId(),
                        record.getReviewLevel(),
                        record.getReviewerId(),
                        reviewerNames.get(record.getReviewerId()),
                        record.getAction() == null ? null : record.getAction().name(),
                        record.getReason(),
                        record.getReviewComment(),
                        record.getCreatedAt()
                ))
                .toList();
    }

    private String displayName(UserEntity user) {
        if (user.getDisplayName() != null) {
            return user.getDisplayName();
        }
        return user.getUsername();
    }
}
