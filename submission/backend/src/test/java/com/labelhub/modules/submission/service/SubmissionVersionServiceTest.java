package com.labelhub.modules.submission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.dto.VersionHistoryItem;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmissionVersionServiceTest {

    @Mock private SubmissionMapper submissionMapper;
    @Mock private AiReviewResultMapper aiReviewResultMapper;
    @Mock private SubmissionUserResolver userResolver;

    @Test
    void getVersionHistoryLoadsAiResultsInBatch() {
        Submission first = submission(100L, 1);
        Submission second = submission(101L, 2);
        AiReviewResult aiResult = new AiReviewResult();
        aiResult.setSubmissionId(101L);
        aiResult.setDecision("PASS");
        aiResult.setFlowAction("HUMAN_REVIEW");
        when(submissionMapper.selectByAssignmentId(10L)).thenReturn(List.of(first, second));
        when(userResolver.resolveCreatorNames(List.of(first, second))).thenReturn(Map.of(7L, "Labeler"));
        when(userResolver.effectiveCreatorId(first)).thenReturn(7L);
        when(userResolver.effectiveCreatorId(second)).thenReturn(7L);
        when(aiReviewResultMapper.selectBySubmissionIds(List.of(100L, 101L)))
                .thenReturn(List.of(aiResult));

        List<VersionHistoryItem> items = service().getVersionHistory(10L);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).aiDecision()).isNull();
        assertThat(items.get(1).aiDecision()).isEqualTo("PASS");
        assertThat(items.get(1).aiFlowAction()).isEqualTo("HUMAN_REVIEW");
        verify(aiReviewResultMapper).selectBySubmissionIds(List.of(100L, 101L));
        verify(aiReviewResultMapper, never()).selectBySubmissionId(100L);
    }

    private SubmissionVersionService service() {
        return new SubmissionVersionService(submissionMapper, aiReviewResultMapper, userResolver);
    }

    private Submission submission(Long id, int versionNo) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setAssignmentId(10L);
        submission.setVersionNo(versionNo);
        submission.setStatus(SubmissionStatus.PENDING_FINAL);
        submission.setLabelerId(7L);
        return submission;
    }
}
