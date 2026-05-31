package com.labelhub.modules.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.labelhub.modules.agent.domain.AgentRun;
import com.labelhub.modules.agent.domain.AgentRunStatus;
import com.labelhub.modules.agent.mapper.AgentRunMapper;
import com.labelhub.modules.ai.domain.AiReviewResult;
import com.labelhub.modules.ai.domain.AiReviewStatus;
import com.labelhub.modules.ai.mapper.AiReviewResultMapper;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.preannotation.domain.PreAnnotation;
import com.labelhub.modules.preannotation.domain.PreAnnotationStatus;
import com.labelhub.modules.preannotation.mapper.PreAnnotationMapper;
import com.labelhub.modules.review.dto.ReviewerSubmissionDetailResponse;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewerSubmissionQueryServiceTest {

    @Mock private SubmissionMapper submissionMapper;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private DatasetItemMapper datasetItemMapper;
    @Mock private TemplateVersionMapper templateVersionMapper;
    @Mock private AiReviewResultMapper aiReviewResultMapper;
    @Mock private AgentRunMapper agentRunMapper;
    @Mock private ReviewRecordMapper reviewRecordMapper;
    @Mock private PreAnnotationMapper preAnnotationMapper;

    private ReviewerSubmissionQueryService service;

    @BeforeEach
    void setUp() {
        service = new ReviewerSubmissionQueryService(submissionMapper, assignmentMapper, datasetItemMapper,
                templateVersionMapper, aiReviewResultMapper, agentRunMapper, reviewRecordMapper, preAnnotationMapper);
    }

    @Test
    void detailIncludesMultimodalAiReviewAndLatestPreAnnotationSummary() {
        when(submissionMapper.selectById(200L)).thenReturn(submission());
        when(assignmentMapper.selectById(10L)).thenReturn(assignment());
        when(datasetItemMapper.selectById(30L)).thenReturn(datasetItem());
        when(templateVersionMapper.selectById(40L)).thenReturn(templateVersion());
        when(aiReviewResultMapper.selectBySubmissionId(200L)).thenReturn(aiReviewResult());
        when(agentRunMapper.selectById(900L)).thenReturn(agentRun());
        when(reviewRecordMapper.selectList(any())).thenReturn(List.of());
        when(submissionMapper.selectList(any())).thenReturn(List.of(submission()));
        when(preAnnotationMapper.selectLatestByAssignmentId(10L)).thenReturn(preAnnotation());

        ReviewerSubmissionDetailResponse response = service.getDetail(200L);

        assertThat(response.aiReviewResult().promptMode()).isEqualTo("IMAGE_SINGLE");
        assertThat(response.aiReviewResult().degraded()).isFalse();
        assertThat(response.aiReviewResult().limitations()).contains("MEDIA_UNCLEAR");
        assertThat(response.latestPreAnnotation()).isNotNull();
        assertThat(response.latestPreAnnotation().promptMode()).isEqualTo("IMAGE_SINGLE");
        assertThat(response.latestPreAnnotation().suggestedAnswerJson()).contains("\"label\":\"cat\"");
        assertThat(response.latestPreAnnotation().finalDiff()).contains("\"finalAnswerJson\"");
    }

    private Submission submission() {
        Submission submission = new Submission();
        submission.setId(200L);
        submission.setAssignmentId(10L);
        submission.setTaskId(20L);
        submission.setDatasetItemId(30L);
        submission.setTemplateVersionId(40L);
        submission.setLabelerId(50L);
        submission.setVersionNo(1);
        submission.setStatus(SubmissionStatus.PENDING_FINAL);
        submission.setAnswerJson("{\"label\":\"final\"}");
        return submission;
    }

    private Assignment assignment() {
        Assignment assignment = new Assignment();
        assignment.setId(10L);
        return assignment;
    }

    private DatasetItem datasetItem() {
        DatasetItem item = new DatasetItem();
        item.setId(30L);
        item.setItemJson("{\"media_type\":\"image\"}");
        return item;
    }

    private TemplateVersion templateVersion() {
        TemplateVersion version = new TemplateVersion();
        version.setId(40L);
        version.setSchemaJson("{\"components\":[]}");
        return version;
    }

    private AiReviewResult aiReviewResult() {
        AiReviewResult result = new AiReviewResult();
        result.setId(700L);
        result.setEffectiveRunId(900L);
        result.setStatus(AiReviewStatus.SUCCESS);
        result.setDecision("MANUAL_REVIEW");
        result.setAverageScore(new BigDecimal("3.5"));
        result.setRiskFlags("[\"MEDIA_UNCLEAR\"]");
        result.setSuggestion("Needs human review");
        result.setPromptMode("IMAGE_SINGLE");
        result.setDegraded(false);
        result.setLimitations("[\"MEDIA_UNCLEAR\"]");
        return result;
    }

    private AgentRun agentRun() {
        AgentRun run = new AgentRun();
        run.setId(900L);
        run.setAgentType("AI_REVIEW");
        run.setStatus(AgentRunStatus.SUCCESS);
        run.setModelName("qwen-vl");
        return run;
    }

    private PreAnnotation preAnnotation() {
        PreAnnotation preAnnotation = new PreAnnotation();
        preAnnotation.setId(800L);
        preAnnotation.setAssignmentId(10L);
        preAnnotation.setAgentRunId(901L);
        preAnnotation.setStatus(PreAnnotationStatus.SUCCESS);
        preAnnotation.setSuggestedAnswerJson("{\"label\":\"cat\"}");
        preAnnotation.setFieldSuggestions("[{\"field\":\"label\"}]");
        preAnnotation.setRiskFlags("[]");
        preAnnotation.setOverallConfidence(new BigDecimal("0.86"));
        preAnnotation.setLimitations("[]");
        preAnnotation.setPromptMode("IMAGE_SINGLE");
        preAnnotation.setDegraded(false);
        preAnnotation.setIgnoredFieldsJson("[]");
        preAnnotation.setMediaUnderstandingJson("{\"usedMedia\":true}");
        return preAnnotation;
    }
}
