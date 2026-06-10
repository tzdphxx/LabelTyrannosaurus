package com.labelhub.modules.submission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.labelhub.common.api.PageResponse;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.dto.LabelerSubmissionListItem;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LabelerSubmissionQueryServiceTest {

    @Mock private SubmissionMapper submissionMapper;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private AiReviewResultMapper aiReviewResultMapper;
    @Mock private ReviewRecordMapper reviewRecordMapper;
    @Mock private DatasetItemMapper datasetItemMapper;
    @Mock private TemplateVersionMapper templateVersionMapper;
    @Mock private SubmissionUserResolver userResolver;

    @Test
    void listSubmissionsPushesAssignmentStatusAndBatchLoadsDetails() {
        Submission submission = submission();
        Assignment assignment = new Assignment();
        assignment.setId(20L);
        assignment.setStatus(AssignmentStatus.RETURNED);
        AiReviewResult aiResult = new AiReviewResult();
        aiResult.setSubmissionId(100L);
        aiResult.setStatus(AiReviewStatus.SUCCESS);
        aiResult.setDecision("REJECT");
        ReviewRecord reject = new ReviewRecord();
        reject.setSubmissionId(100L);
        reject.setReason("missing field");

        when(submissionMapper.countLabelerSubmissions(
                7L, 10L, "REJECTED", "RETURNED", false)).thenReturn(1L);
        when(submissionMapper.selectLabelerSubmissionsPage(
                7L, 10L, "REJECTED", "RETURNED", false, 20, 0))
                .thenReturn(List.of(submission));
        when(assignmentMapper.selectList(ArgumentMatchers.<Wrapper<Assignment>>any()))
                .thenReturn(List.of(assignment));
        when(aiReviewResultMapper.selectBySubmissionIds(List.of(100L))).thenReturn(List.of(aiResult));
        when(reviewRecordMapper.selectLatestRejectBySubmissionIds(List.of(100L))).thenReturn(List.of(reject));

        PageResponse<LabelerSubmissionListItem> response = service().listSubmissions(
                7L, 10L, SubmissionStatus.REJECTED, AssignmentStatus.RETURNED, 1, 20);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).assignmentStatus()).isEqualTo(AssignmentStatus.RETURNED);
        assertThat(response.items().get(0).aiDecision()).isEqualTo("REJECT");
        assertThat(response.items().get(0).rejectReason()).isEqualTo("missing field");
        verify(submissionMapper).countLabelerSubmissions(
                7L, 10L, "REJECTED", "RETURNED", false);
        verify(aiReviewResultMapper).selectBySubmissionIds(List.of(100L));
        verify(reviewRecordMapper).selectLatestRejectBySubmissionIds(List.of(100L));
    }

    private LabelerSubmissionQueryService service() {
        return new LabelerSubmissionQueryService(submissionMapper, assignmentMapper,
                aiReviewResultMapper, reviewRecordMapper, datasetItemMapper,
                templateVersionMapper, userResolver);
    }

    private Submission submission() {
        Submission submission = new Submission();
        submission.setId(100L);
        submission.setAssignmentId(20L);
        submission.setTaskId(10L);
        submission.setDatasetItemId(30L);
        submission.setVersionNo(1);
        submission.setStatus(SubmissionStatus.REJECTED);
        submission.setIsGolden(false);
        return submission;
    }
}
